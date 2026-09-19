"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { won } from "@/lib/performances";
import AccountMenu from "@/components/account-menu";

type ReservationItem = {
  reservationId: number;
  orderId: string;
  status: string;
  totalAmount: number;
  createdAt: string;
  paidAt: string | null;
  performanceTitle: string;
  category: string;
  venue: string;
  color: string;
  scheduleDate: string;
  scheduleTime: string;
  seatNumbers: string[];
};

export default function MyReservationsPage() {
  const [list, setList] = useState<ReservationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    async function load() {
      try {
        const res = await api.get("/api/reservations/my");
        const data = await res.json();
        if (active) {
          setList(data);
          setError("");
        }
      } catch (err) {
        if (active) {
          if (err instanceof ApiError && err.status === 401) {
            setError("LOGIN_REQUIRED");
          } else {
            setError(err instanceof Error ? err.message : "예매 내역을 불러오지 못했습니다.");
          }
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="home-page">
      <header className="site-header">
        <Link className="brand" href="/">
          TICKET <span>ON</span>
        </Link>
        <AccountMenu />
      </header>

      <main className="booking-main" style={{ maxWidth: "800px", margin: "32px auto", padding: "0 20px" }}>
        <Link className="back-link" href="/">
          ← 메인 화면으로
        </Link>

        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "8px" }}>
          <h1 style={{ fontSize: "1.75rem", margin: 0 }}>내 예매 내역</h1>
          <span style={{ color: "var(--color-muted, #888)", fontSize: "0.9rem" }}>총 {list.length}건</span>
        </div>

        {loading ? (
          <div style={{ textAlign: "center", padding: "80px 20px", color: "var(--color-muted, #888)" }}>
            예매 내역을 불러오는 중…
          </div>
        ) : error === "LOGIN_REQUIRED" ? (
          <div style={{ textAlign: "center", padding: "80px 20px" }}>
            <div style={{ fontSize: "2.5rem", marginBottom: "16px" }}>🔒</div>
            <h2>로그인이 필요한 페이지입니다</h2>
            <p style={{ color: "var(--color-muted, #888)", marginBottom: "24px" }}>
              내 예매 내역을 확인하시려면 먼저 로그인해 주세요.
            </p>
            <Link
              href="/login"
              style={{
                display: "inline-block",
                padding: "12px 24px",
                borderRadius: "10px",
                background: "var(--accent, #3b82f6)",
                color: "#fff",
                fontWeight: 600,
                textDecoration: "none",
              }}
            >
              로그인하러 가기 ↗
            </Link>
          </div>
        ) : error ? (
          <div style={{ textAlign: "center", padding: "60px 20px", color: "#ef4444" }}>{error}</div>
        ) : list.length === 0 ? (
          <div
            style={{
              textAlign: "center",
              padding: "80px 20px",
              background: "var(--surface, rgba(255,255,255,0.02))",
              border: "1px solid var(--border, rgba(255,255,255,0.08))",
              borderRadius: "16px",
            }}
          >
            <div style={{ fontSize: "3rem", marginBottom: "16px" }}>🎟️</div>
            <h2>아직 예매하신 티켓이 없어요</h2>
            <p style={{ color: "var(--color-muted, #888)", marginBottom: "24px" }}>
              설레는 순간을 놓치지 않도록 티켓온의 다양한 공연을 둘러보세요.
            </p>
            <Link
              href="/"
              style={{
                display: "inline-block",
                padding: "10px 20px",
                borderRadius: "8px",
                background: "var(--accent, #3b82f6)",
                color: "#fff",
                fontWeight: 600,
                textDecoration: "none",
              }}
            >
              공연 둘러보기 ↗
            </Link>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            {list.map((item) => {
              const isConfirmed = item.status === "CONFIRMED";
              const isPending = item.status === "PENDING_PAYMENT";

              return (
                <div
                  key={item.reservationId}
                  style={{
                    background: "var(--surface, rgba(255, 255, 255, 0.03))",
                    border: "1px solid var(--border, rgba(255, 255, 255, 0.1))",
                    borderRadius: "16px",
                    padding: "20px 24px",
                    display: "flex",
                    flexDirection: "column",
                    gap: "12px",
                    transition: "transform 0.15s ease, border-color 0.15s ease",
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                      <span
                        style={{
                          fontSize: "0.75rem",
                          padding: "2px 8px",
                          borderRadius: "6px",
                          background: "rgba(255,255,255,0.08)",
                          color: "var(--color-muted, #aaa)",
                        }}
                      >
                        {item.category}
                      </span>
                      <span style={{ fontSize: "0.85rem", color: "var(--color-muted, #888)" }}>
                        주문번호: {item.orderId}
                      </span>
                    </div>

                    <span
                      style={{
                        padding: "4px 10px",
                        borderRadius: "999px",
                        fontSize: "0.8rem",
                        fontWeight: 600,
                        background: isConfirmed
                          ? "rgba(34, 197, 94, 0.15)"
                          : isPending
                          ? "rgba(245, 158, 11, 0.15)"
                          : "rgba(239, 68, 68, 0.15)",
                        color: isConfirmed ? "#22c55e" : isPending ? "#f59e0b" : "#ef4444",
                      }}
                    >
                      {isConfirmed ? "예매 완료" : isPending ? "결제 대기" : "예매 취소"}
                    </span>
                  </div>

                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div>
                      <h2 style={{ fontSize: "1.25rem", margin: "0 0 6px 0" }}>{item.performanceTitle}</h2>
                      <p style={{ margin: 0, color: "var(--color-muted, #aaa)", fontSize: "0.95rem" }}>
                        {item.venue} · {item.scheduleDate.replaceAll("-", ".")} {item.scheduleTime}
                      </p>
                    </div>

                    <div style={{ textAlign: "right" }}>
                      <span style={{ display: "block", fontSize: "0.8rem", color: "var(--color-muted, #888)" }}>
                        결제 금액
                      </span>
                      <strong style={{ fontSize: "1.25rem", color: "var(--accent, #3b82f6)" }}>
                        {won(item.totalAmount)}
                      </strong>
                    </div>
                  </div>

                  <div
                    style={{
                      borderTop: "1px dashed var(--border, rgba(255, 255, 255, 0.08))",
                      paddingTop: "10px",
                      display: "flex",
                      justifyContent: "space-between",
                      fontSize: "0.875rem",
                    }}
                  >
                    <span>
                      좌석:{" "}
                      <strong style={{ color: "#38bdf8" }}>
                        {item.seatNumbers.length > 0 ? item.seatNumbers.join(", ") : "지정 좌석 없음"}
                      </strong>{" "}
                      (총 {item.seatNumbers.length}매)
                    </span>
                    <span style={{ color: "var(--color-muted, #666)" }}>
                      예매일: {item.createdAt ? new Date(item.createdAt).toLocaleDateString("ko-KR") : "-"}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>

      <footer className="site-footer">
        <span>© TICKET ON</span>
        <span>당신의 모든 설레는 순간과 함께.</span>
      </footer>
    </div>
  );
}
