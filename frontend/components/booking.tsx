"use client";

import Link from "next/link";
import { useState } from "react";
import { type Performance, won } from "@/lib/performances";

export default function Booking({ performance: show }: { performance: Performance }) {
  const [date, setDate] = useState<string>(show.dates[0]);
  const [time, setTime] = useState("14:00");
  const [seats, setSeats] = useState<string[]>([]);
  const [quantity, setQuantity] = useState(1);
  const [message, setMessage] = useState("");
  const assigned = show.category === "콘서트" || show.category === "뮤지컬";
  const count = assigned ? seats.length : quantity;
  function reset() { setSeats([]); setMessage(""); }
  function toggle(seat: string) {
    if (!seats.includes(seat) && seats.length >= 4) { setMessage("좌석은 최대 4개까지 선택할 수 있어요."); return; }
    setSeats(seats.includes(seat) ? seats.filter((item) => item !== seat) : [...seats, seat]);
    setMessage("");
  }
  return <div className="home-page">
    <header className="site-header"><Link className="brand" href="/">TICKET <span>ON</span></Link><Link href="/login">로그인 ↗</Link></header>
    <main className="booking-main"><Link className="back-link" href="/">← 공연 목록으로</Link><p className="preview-notice">예약 체험 화면입니다. 공연·좌석은 예시이며 실제 예약이나 결제가 진행되지 않습니다.</p>
      <div className="booking-layout"><div>
        <section className="show-detail"><div className={`show-poster detail-poster ${show.color}`} aria-hidden="true"><strong>{show.subtitle}</strong><span className="poster-symbol">{show.symbol}</span></div><div><span className="show-category">{show.category}</span><h1>{show.title}</h1><p>{show.tag}</p><dl><dt>장소</dt><dd>{show.venue}</dd><dt>기간</dt><dd>{show.date}</dd><dt>가격</dt><dd>{won(show.price)}</dd></dl></div></section>
        <section className="booking-section"><h2><span>01</span> 날짜와 회차를 선택해 주세요</h2><div className="booking-fields"><label>관람 날짜<select value={date} onChange={(event) => { setDate(event.target.value); reset(); }}>{show.dates.map((day) => <option key={day} value={day}>{day.replaceAll("-", ".")}</option>)}</select></label><label>관람 시간<select value={time} onChange={(event) => { setTime(event.target.value); reset(); }}><option>14:00</option><option>19:00</option></select></label></div></section>
        <section className="booking-section"><h2><span>02</span> {assigned ? "좌석을 선택해 주세요" : "관람 인원을 선택해 주세요"}</h2>{assigned ? <><p className="booking-help">최대 4석까지 선택할 수 있어요. 모든 좌석은 동일 가격입니다.</p><div className="stage">S T A G E</div><div className="seat-map">{["A", "B", "C", "D"].map((row) => <div className="seat-row" key={row}><span>{row}</span>{Array.from({ length: 8 }, (_, index) => { const seat = `${row}${index + 1}`; const unavailable = ["A3", "A4", "B6", "C2"].includes(seat); return <button key={seat} disabled={unavailable} aria-label={`${seat} 좌석${unavailable ? " 선택 불가" : ""}`} aria-pressed={seats.includes(seat)} onClick={() => toggle(seat)}>{index + 1}</button>; })}</div>)}</div><div className="seat-legend"><span>□ 선택 가능</span><span>■ 선택 불가</span><span>🟦 선택한 좌석</span></div></> : <label className="quantity-label">일반 입장권<select value={quantity} onChange={(event) => { setQuantity(Number(event.target.value)); setMessage(""); }}>{[1, 2, 3, 4].map((number) => <option key={number} value={number}>{number}매</option>)}</select></label>}</section>
      </div><aside className="booking-summary"><span className="eyebrow">YOUR NEXT MOMENT</span><h2>선택 내역</h2><h3>{show.title}</h3><p>{date.replaceAll("-", ".")} · {time}</p><p>{show.venue}</p><div className="selected-seats">{assigned ? seats.length ? seats.join(", ") : "좌석을 선택해 주세요" : `일반 입장권 ${quantity}매`}</div><div className="summary-total"><span>총 {count}매</span><strong>{won(show.price * count)}</strong></div><button className="submit-button" disabled={!count} onClick={() => setMessage("선택 내역을 확인했어요. 실제 예매 기능은 준비 중이며, 좌석은 확보되지 않았습니다.")}>선택 내역 확인</button><p className="booking-help">예매 수수료는 포함되지 않은 예시 금액입니다.</p><p className="booking-message" role="status" aria-live="polite">{message}</p></aside></div>
    </main><footer className="site-footer"><span>© TICKET ON</span><span>당신의 모든 설레는 순간과 함께.</span></footer>
  </div>;
}
