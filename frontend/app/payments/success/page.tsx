"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { won } from "@/lib/performances";

type ConfirmedInfo = {
  success: boolean;
  reservationId: number;
  orderId: string;
  orderName: string;
  amount: number;
  status: string;
  paidAt: string;
  seatNumbers: string[];
};

function SuccessContent() {
  const searchParams = useSearchParams();
  const paymentKey = searchParams.get("paymentKey");
  const orderId = searchParams.get("orderId");
  const amount = searchParams.get("amount");

  const [loading, setLoading] = useState(true);
  const [confirmed, setConfirmed] = useState<ConfirmedInfo | null>(null);
  const [error, setError] = useState<string>("");

  useEffect(() => {
    if (!paymentKey || !orderId || !amount) {
      setError("결제 승인 정보가 올바르지 않습니다.");
      setLoading(false);
      return;
    }

    let active = true;

    async function confirm() {
      try {
        const res = await api.post("/api/payments/confirm", {
          paymentKey,
          orderId,
          amount: Number(amount),
        });
        const data: ConfirmedInfo = await res.json();
        if (active) {
          setConfirmed(data);
          setError("");
        }
      } catch (err) {
        if (active) {
          setError(
            err instanceof ApiError
              ? err.message
              : err instanceof Error
              ? err.message
              : "결제 승인 처리 중 오류가 발생했습니다."
          );
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    void confirm();

    return () => {
      active = false;
    };
  }, [paymentKey, orderId, amount]);

  return (
    <div className="home-page">
      <header className="site-header">
        <Link className="brand" href="/">
          TICKET <span>ON</span>
        </Link>
        <Link href="/my/reservations">내 예매 내역 ↗</Link>
      </header>

      <main className="booking-main" style={{ maxWidth: "640px", margin: "40px auto" }}>
        {loading ? (
          <div style={{ textAlign: "center", padding: "60px 20px" }}>
            <div style={{ fontSize: "2rem", marginBottom: "16px" }}>⏳</div>
            <h2>토스페이먼츠 승인 처리 중…</h2>
            <p style={{ color: "var(--color-muted, #888)", marginTop: "8px" }}>
              결제 위변조 검증 및 좌석 확정을 안전하게 진행하고 있습니다.
            </p>
          </div>
        ) : error ? (
          <div style={{ textAlign: "center", padding: "60px 20px" }}>
            <div style={{ fontSize: "2rem", marginBottom: "16px" }}>⚠️</div>
            <h2>결제 승인 실패</h2>
            <p style={{ color: "#ef4444", marginTop: "8px", marginBottom: "24px" }}>{error}</p>
            <div style={{ display: "flex", gap: "12px", justifyContent: "center" }}>
              <Link
                href="/"
                style={{
                  padding: "10px 20px",
                  borderRadius: "8px",
                  background: "var(--accent, #3b82f6)",
                  color: "#fff",
                  fontWeight: 600,
                  textDecoration: "none",
                }}
              >
                공연 목록으로 돌아가기
              </Link>
            </div>
          </div>
        ) : confirmed ? (
          <div style={{ textAlign: "center", padding: "40px 20px" }}>
            <div style={{ fontSize: "3rem", marginBottom: "16px" }}>🎉</div>
            <span
              style={{
                display: "inline-block",
                padding: "4px 12px",
                borderRadius: "999px",
                background: "rgba(34, 197, 94, 0.15)",
                color: "#22c55e",
                fontWeight: 600,
                fontSize: "0.875rem",
                marginBottom: "12px",
              }}
            >
              예매 완료
            </span>
            <h1 style={{ fontSize: "1.75rem", marginBottom: "8px" }}>예매가 정상적으로 완료되었습니다!</h1>
            <p style={{ color: "var(--color-muted, #888)", marginBottom: "32px" }}>
              선택하신 좌석이 확정되었습니다. 관람 당일 본인 확인 후 입장 가능합니다.
            </p>

            <div
              style={{
                textAlign: "left",
                background: "var(--surface, rgba(255,255,255,0.03))",
                border: "1px solid var(--border, rgba(255,255,255,0.1))",
                borderRadius: "16px",
                padding: "24px",
                marginBottom: "32px",
                display: "flex",
                flexDirection: "column",
                gap: "12px",
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span style={{ color: "var(--color-muted, #888)" }}>주문명</span>
                <strong style={{ textAlign: "right" }}>{confirmed.orderName}</strong>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span style={{ color: "var(--color-muted, #888)" }}>확정 좌석</span>
                <strong style={{ color: "#3b82f6" }}>{confirmed.seatNumbers.join(", ")}</strong>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span style={{ color: "var(--color-muted, #888)" }}>주문 번호</span>
                <span>{confirmed.orderId}</span>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span style={{ color: "var(--color-muted, #888)" }}>예약 번호</span>
                <span>#{confirmed.reservationId}</span>
              </div>
              <div
                style={{
                  borderTop: "1px dashed var(--border, rgba(255,255,255,0.1))",
                  marginTop: "8px",
                  paddingTop: "12px",
                  display: "flex",
                  justifyContent: "space-between",
                  fontSize: "1.1rem",
                }}
              >
                <span>최종 결제 금액</span>
                <strong style={{ color: "var(--accent, #3b82f6)" }}>{won(confirmed.amount)}</strong>
              </div>
            </div>

            <div style={{ display: "flex", gap: "12px", justifyContent: "center" }}>
              <Link
                href="/my/reservations"
                style={{
                  padding: "12px 24px",
                  borderRadius: "10px",
                  background: "var(--accent, #3b82f6)",
                  color: "#fff",
                  fontWeight: 600,
                  textDecoration: "none",
                }}
              >
                내 예매 내역 보러가기 ↗
              </Link>
              <Link
                href="/"
                style={{
                  padding: "12px 24px",
                  borderRadius: "10px",
                  background: "var(--surface, rgba(255,255,255,0.06))",
                  color: "var(--text, #fff)",
                  border: "1px solid var(--border, rgba(255,255,255,0.1))",
                  fontWeight: 600,
                  textDecoration: "none",
                }}
              >
                공연 목록으로
              </Link>
            </div>
          </div>
        ) : null}
      </main>
    </div>
  );
}

export default function PaymentSuccessPage() {
  return (
    <Suspense fallback={<div style={{ textAlign: "center", padding: "60px" }}>불러오는 중…</div>}>
      <SuccessContent />
    </Suspense>
  );
}
