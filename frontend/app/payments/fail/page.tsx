"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense } from "react";

function FailContent() {
  const searchParams = useSearchParams();
  const code = searchParams.get("code");
  const message = searchParams.get("message");
  const orderId = searchParams.get("orderId");

  return (
    <div className="home-page">
      <header className="site-header">
        <Link className="brand" href="/">
          TICKET <span>ON</span>
        </Link>
      </header>

      <main className="booking-main" style={{ maxWidth: "560px", margin: "60px auto", textAlign: "center" }}>
        <div style={{ fontSize: "3rem", marginBottom: "16px" }}>❌</div>
        <h1 style={{ fontSize: "1.75rem", marginBottom: "8px" }}>결제가 취소되었거나 실패했습니다</h1>
        <p style={{ color: "#ef4444", marginBottom: "24px" }}>
          {message ?? "사용자가 결제를 취소했거나 승인에 실패했습니다."}
        </p>

        {code && (
          <p style={{ color: "var(--color-muted, #888)", fontSize: "0.875rem", marginBottom: "32px" }}>
            에러 코드: {code} {orderId ? `(주문번호: ${orderId})` : ""}
          </p>
        )}

        <div style={{ display: "flex", gap: "12px", justifyContent: "center" }}>
          <Link
            href="/"
            style={{
              padding: "12px 24px",
              borderRadius: "10px",
              background: "var(--accent, #3b82f6)",
              color: "#fff",
              fontWeight: 600,
              textDecoration: "none",
            }}
          >
            공연 목록으로 돌아가기
          </Link>
        </div>
      </main>
    </div>
  );
}

export default function PaymentFailPage() {
  return (
    <Suspense fallback={<div style={{ textAlign: "center", padding: "60px" }}>불러오는 중…</div>}>
      <FailContent />
    </Suspense>
  );
}
