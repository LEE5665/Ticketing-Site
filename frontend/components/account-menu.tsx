"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { api, currentMember, type CurrentMember } from "@/lib/api";

export default function AccountMenu() {
  const [member, setMember] = useState<CurrentMember | null>(null);
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const busy = useRef(false);

  useEffect(() => {
    let active = true;
    void currentMember().then((result) => { if (active) setMember(result); })
      .catch(() => { if (active) setError("로그인 상태를 확인하지 못했어요."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  async function logout() {
    if (busy.current) return;
    busy.current = true;
    setPending(true);
    setError("");
    try {
      await api.post("/api/auth/logout");
      setMember(null);
    } catch (error) {
      setError(error instanceof Error ? error.message : "로그아웃에 실패했어요.");
    } finally {
      busy.current = false;
      setPending(false);
    }
  }

  return <div className="account-area">
    <nav className="account-nav" aria-label="계정">
      {loading ? <span role="status">확인 중…</span> : member ? <>
        <span className="member-name">{member.name}님</span>
        <Link href="/my/reservations" className="my-reservations-link">내 예매</Link>
        <button disabled={pending} onClick={logout}>{pending ? "처리 중…" : "로그아웃"}</button>
      </> : <><Link href="/login">로그인</Link><Link href="/signup">회원가입 ↗</Link></>}
    </nav>
    {error && <p className="account-error" role="status">{error}</p>}
  </div>;
}
