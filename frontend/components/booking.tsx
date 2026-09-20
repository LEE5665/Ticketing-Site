"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { type Performance, type SeatInfo, fetchSeats, won } from "@/lib/performances";
import { api, ApiError } from "@/lib/api";
import AccountMenu from "@/components/account-menu";
import { loadTossPayments, ANONYMOUS } from "@tosspayments/tosspayments-sdk";

const TOSS_CLIENT_KEY = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY ?? "test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm";

export default function Booking({ performance: show }: { performance: Performance }) {
  // 백엔드에서 전달받은 schedules로부터 고유 날짜 목록 추출
  const availableDates = Array.from(new Set(show.schedules?.map((s) => s.date) ?? []));
  const [date, setDate] = useState<string>(availableDates[0] ?? "");

  // 선택된 날짜에 열려있는 시간 목록
  const availableTimes = show.schedules?.filter((s) => s.date === date).map((s) => s.time) ?? [];
  const [time, setTime] = useState<string>(availableTimes[0] ?? "");

  const [seatList, setSeatList] = useState<SeatInfo[]>([]);
  const [seats, setSeats] = useState<string[]>([]);
  const [quantity, setQuantity] = useState(1);
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // 미결제 가예약 번호 추적 및 결제 진행 플래그
  const pendingOrderIdRef = useRef<string | null>(null);
  const isPayingRef = useRef(false);

  // 페이지 이탈(뒤로가기, 메인 이동 등) 시 결제 완료되지 않은 가예약 자동 취소
  useEffect(() => {
    return () => {
      if (!isPayingRef.current && pendingOrderIdRef.current) {
        void api.post(`/api/reservations/${pendingOrderIdRef.current}/cancel`);
      }
    };
  }, []);

  // 현재 선택된 회차(Schedule)
  const currentSchedule = show.schedules?.find((s) => s.date === date && s.time === time);

  const assigned = show.category === "콘서트" || show.category === "뮤지컬";
  const count = assigned ? seats.length : quantity;

  // 회차가 바뀌면 백엔드 API에서 최신 좌석 배치 현황을 실시간 조회
  useEffect(() => {
    if (!currentSchedule?.id) return;
    let active = true;
    void fetchSeats(currentSchedule.id)
      .then((data) => {
        if (active) {
          setSeatList(data);
          setSeats([]);
          setMessage("");
        }
      })
      .catch(() => {
        if (active) setMessage("좌석 정보를 불러오지 못했어요.");
      });
    return () => {
      active = false;
    };
  }, [currentSchedule?.id]);

  function reset() {
    setSeats([]);
    setMessage("");
  }

  function toggle(seat: string) {
    if (!seats.includes(seat) && seats.length >= 4) {
      setMessage("좌석은 최대 4개까지 선택할 수 있어요.");
      return;
    }
    setSeats(seats.includes(seat) ? seats.filter((item) => item !== seat) : [...seats, seat]);
    setMessage("");
  }

  async function handleCheckout() {
    if (!currentSchedule?.id) {
      setMessage("회차를 먼저 선택해 주세요.");
      return;
    }

    let targetSeats = seats;
    if (!assigned) {
      const availableSeats = seatList.filter((s) => s.available).slice(0, quantity).map((s) => s.seatNumber);
      if (availableSeats.length < quantity) {
        setMessage("남은 티켓 수량이 부족합니다.");
        return;
      }
      targetSeats = availableSeats;
    }

    if (targetSeats.length === 0) {
      setMessage("좌석을 선택해 주세요.");
      return;
    }

    setSubmitting(true);
    setMessage("");

    try {
      // 1. 가예약 생성 (비관적 락으로 5분 선점)
      const res = await api.post(`/api/schedules/${currentSchedule.id}/reservations/simple`, {
        seatNumbers: targetSeats,
      });
      const data = await res.json();
      pendingOrderIdRef.current = data.orderId;

      // 2. 토스페이먼츠 SDK 호출
      isPayingRef.current = true;
      const tossPayments = await loadTossPayments(TOSS_CLIENT_KEY);

      if (TOSS_CLIENT_KEY.startsWith("test_gck_")) {
        // 토스 공식 문서 공개 키 (결제위젯 / 창형)
        const widgets = tossPayments.widgets({ customerKey: ANONYMOUS });
        await widgets.setAmount({ currency: "KRW", value: data.amount });
        await widgets.requestPaymentWindow({
          amount: {
            currency: "KRW",
            value: data.amount,
          },
          orderId: data.orderId,
          orderName: data.orderName,
          successUrl: `${window.location.origin}/payments/success`,
          failUrl: `${window.location.origin}/payments/fail`,
          customerEmail: data.customerEmail,
          customerName: data.customerName,
        });
      } else {
        // 개별 연동 키 (test_ck_...)
        const payment = tossPayments.payment({ customerKey: ANONYMOUS });
        await payment.requestPayment({
          method: "CARD",
          amount: {
            currency: "KRW",
            value: data.amount,
          },
          orderId: data.orderId,
          orderName: data.orderName,
          successUrl: `${window.location.origin}/payments/success`,
          failUrl: `${window.location.origin}/payments/fail`,
          customerEmail: data.customerEmail,
          customerName: data.customerName,
        });
      }
    } catch (error) {
      console.error("Toss requestPayment error:", error);
      isPayingRef.current = false;

      // 결제창 닫기나 에러 발생 시 가예약 즉시 취소 및 좌석 해제
      if (pendingOrderIdRef.current) {
        void api.post(`/api/reservations/${pendingOrderIdRef.current}/cancel`);
        pendingOrderIdRef.current = null;
        if (currentSchedule?.id) {
          void fetchSeats(currentSchedule.id).then(setSeatList);
        }
      }

      if (error instanceof ApiError && error.status === 401) {
        setMessage("로그인이 필요합니다. 상단 우측에서 먼저 로그인해 주세요.");
      } else {
        setMessage(error instanceof Error ? error.message : "예매 처리에 실패했습니다.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="home-page">
      <header className="site-header">
        <Link className="brand" href="/">
          TICKET <span>ON</span>
        </Link>
        <AccountMenu />
      </header>
      <main className="booking-main">
        <Link className="back-link" href="/">
          ← 공연 목록으로
        </Link>
        <p className="preview-notice">
          실시간 좌석 연동 화면입니다. 회차를 선택하면 백엔드 DB의 실시간 좌석 상태가 반영됩니다.
        </p>
        <div className="booking-layout">
          <div>
            <section className="show-detail">
              <div className={`show-poster detail-poster ${show.color}`} aria-hidden="true">
                <strong>{show.subtitle}</strong>
                <span className="poster-symbol">{show.symbol}</span>
              </div>
              <div>
                <span className="show-category">{show.category}</span>
                <h1>{show.title}</h1>
                <p>{show.tag}</p>
                <dl>
                  <dt>장소</dt>
                  <dd>{show.venue}</dd>
                  <dt>기간</dt>
                  <dd>{show.date}</dd>
                  <dt>가격</dt>
                  <dd>{won(show.price)}</dd>
                </dl>
              </div>
            </section>
            <section className="booking-section">
              <h2>
                <span>01</span> 날짜와 회차를 선택해 주세요
              </h2>
              <div className="booking-fields">
                <label>
                  관람 날짜
                  <select
                    value={date}
                    onChange={(event) => {
                      const newDate = event.target.value;
                      setDate(newDate);
                      const nextTimes = show.schedules?.filter((s) => s.date === newDate).map((s) => s.time) ?? ["14:00"];
                      setTime(nextTimes[0] ?? "14:00");
                      reset();
                    }}
                  >
                    {availableDates.map((day) => (
                      <option key={day} value={day}>
                        {day.replaceAll("-", ".")}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  관람 시간
                  <select
                    value={time}
                    onChange={(event) => {
                      setTime(event.target.value);
                      reset();
                    }}
                  >
                    {availableTimes.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
            </section>
            <section className="booking-section">
              <h2>
                <span>02</span> {assigned ? "좌석을 선택해 주세요" : "관람 인원을 선택해 주세요"}
              </h2>
              {assigned ? (
                <>
                  <p className="booking-help">
                    최대 4석까지 선택할 수 있어요. 모든 좌석은 동일 가격입니다.
                  </p>
                  <div className="stage">S T A G E</div>
                  <div className="seat-map">
                    {["A", "B", "C", "D"].map((row) => (
                      <div className="seat-row" key={row}>
                        <span>{row}</span>
                        {Array.from({ length: 8 }, (_, index) => {
                          const seat = `${row}${index + 1}`;
                          // 백엔드 API에서 내려온 실시간 예약 가능 여부 확인
                          const seatData = seatList.find((s) => s.seatNumber === seat);
                          const unavailable = seatData ? !seatData.available : false;

                          return (
                            <button
                              key={seat}
                              disabled={unavailable}
                              aria-label={`${seat} 좌석${unavailable ? " 선택 불가" : ""}`}
                              aria-pressed={seats.includes(seat)}
                              onClick={() => toggle(seat)}
                            >
                              {index + 1}
                            </button>
                          );
                        })}
                      </div>
                    ))}
                  </div>
                  <div className="seat-legend">
                    <span>□ 선택 가능</span>
                    <span>■ 이미 예매됨(불가)</span>
                    <span>🟦 선택한 좌석</span>
                  </div>
                </>
              ) : (
                <label className="quantity-label">
                  일반 입장권
                  <select
                    value={quantity}
                    onChange={(event) => {
                      setQuantity(Number(event.target.value));
                      setMessage("");
                    }}
                  >
                    {[1, 2, 3, 4].map((number) => (
                      <option key={number} value={number}>
                        {number}매
                      </option>
                    ))}
                  </select>
                </label>
              )}
            </section>
          </div>
          <aside className="booking-summary">
            <span className="eyebrow">YOUR NEXT MOMENT</span>
            <h2>선택 내역</h2>
            <h3>{show.title}</h3>
            <p>
              {date.replaceAll("-", ".")} · {time}
            </p>
            <p>{show.venue}</p>
            <div className="selected-seats">
              {assigned
                ? seats.length
                  ? seats.join(", ")
                  : "좌석을 선택해 주세요"
                : `일반 입장권 ${quantity}매`}
            </div>
            <div className="summary-total">
              <span>총 {count}매</span>
              <strong>{won(show.price * count)}</strong>
            </div>
            <button
              className="submit-button"
              disabled={!count || submitting}
              onClick={handleCheckout}
            >
              {submitting ? "결제창 연결 중…" : "토스페이로 결제하기"}
            </button>
            <p className="booking-help">예매 수수료는 포함되지 않은 금액입니다.</p>
            <p className="booking-message" role="status" aria-live="polite">
              {message}
            </p>
          </aside>
        </div>
      </main>
      <footer className="site-footer">
        <span>© TICKET ON</span>
        <span>당신의 모든 설레는 순간과 함께.</span>
      </footer>
    </div>
  );
}
