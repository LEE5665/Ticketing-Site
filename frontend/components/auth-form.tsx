"use client";

import Link from "next/link";
import { useRef, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";

export default function AuthForm({ mode, registered = false }: { mode: "login" | "signup"; registered?: boolean }) {
  const joining = mode === "signup";
  const [visible, setVisible] = useState(false);
  const [message, setMessage] = useState("");
  const [pending, setPending] = useState(false);
  const submitting = useRef(false);
  const router = useRouter();

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current) return;
    const form = event.currentTarget;
    const data = new FormData(event.currentTarget);
    const password = String(data.get("password"));
    if (joining && !String(data.get("name")).trim()) {
      setMessage("이름을 입력해 주세요.");
      return;
    }
    if (joining && new TextEncoder().encode(password).length > 72) {
      setMessage("비밀번호가 너무 길어요. 조금 줄여 주세요.");
      return;
    }
    if (joining && password !== data.get("confirmPassword")) {
      setMessage("비밀번호가 일치하지 않아요. 다시 확인해 주세요.");
      return;
    }
    submitting.current = true;
    setPending(true);
    setMessage("");
    try {
      const email = String(data.get("email")).trim().toLowerCase();
      if (joining) {
        await api.post("/api/members", {
          name: String(data.get("name")).trim(), email, password,
        });
        form.reset();
        router.replace("/login?registered=1");
      } else {
        await api.post("/api/auth/login", new URLSearchParams({ email, password }));
        form.reset();
        router.replace("/");
        router.refresh();
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "요청에 실패했어요.");
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }

  return (
    <div className="auth-page">
      <header className="site-header">
        <Link className="brand" href="/" aria-label="티켓온 홈"><span className="brand-mark" aria-hidden="true">t</span>TICKET <span>ON</span><i /></Link>
        <span className="header-note">좋아하는 순간을 더 가까이</span>
      </header>
      <main className="auth-main">
        <section className="intro" aria-label="티켓온 소개">
          <p className="eyebrow">● &nbsp; YOUR NEXT MOMENT</p>
          <h1>기다려온 순간,<br />당신의 자리에서.</h1>
          <p className="intro-copy">가슴 뛰는 첫 음부터 마지막 커튼콜까지.<br />잊지 못할 순간의 시작, 티켓온과 함께하세요.</p>
          <div className="ticket-scene" aria-hidden="true">
            <div className="orbit" /><span className="star">✳</span>
            <div className="ticket">
              <div className="ticket-top">TICKET ON PRESENTS <span>↗</span></div>
              <div className="ticket-title">MAKE IT<br /><span>LIVE.</span></div>
              <p>당신의 일상에, 잊지 못할 한 장면.</p>
              <div className="ticket-stub"><div><small>ADMIT ONE</small><strong>YOUR SPECIAL MOMENT</strong></div><div className="barcode" /></div>
            </div>
            <span className="ticket-badge">취향이 만나는 순간 &nbsp; ↗</span>
          </div>
          <p className="categories">CONCERT &nbsp; · &nbsp; MUSICAL &nbsp; · &nbsp; FESTIVAL</p>
        </section>
        <section className="form-panel" aria-labelledby="form-title">
          <span className="eyebrow">{joining ? "JOIN THE MOMENT" : "WELCOME BACK"}</span>
          <h2 id="form-title">{joining ? "새로운 순간을 함께해요" : "다시 만나 반가워요"}</h2>
          <p className="form-description">{joining ? "티켓온에서 나만의 특별한 순간을 찾아보세요." : "로그인하고 기다려온 공연을 만나보세요."}</p>
          <nav className="auth-tabs" aria-label="계정 메뉴">
            <Link href="/login" aria-current={!joining ? "page" : undefined}>로그인</Link>
            <Link href="/signup" aria-current={joining ? "page" : undefined}>회원가입</Link>
          </nav>
          <form onSubmit={submit} onChange={() => setMessage("")} aria-busy={pending}>
            {!joining && registered && <p className="auth-success" role="status">회원가입이 완료되었어요. 로그인해 주세요.</p>}
            <fieldset disabled={pending} className="auth-fieldset">
            {joining && <div className="field"><label htmlFor="name">이름</label><input id="name" name="name" autoComplete="name" placeholder="이름을 입력해 주세요" required maxLength={100} /></div>}
            <div className="field"><label htmlFor="email">이메일</label><input id="email" name="email" type="email" autoComplete="email" placeholder="hello@example.com" required maxLength={254} /></div>
            <div className="field">
              <label htmlFor="password">비밀번호</label>
              <div className="password-input"><input id="password" name="password" type={visible ? "text" : "password"} autoComplete={joining ? "new-password" : "current-password"} placeholder={joining ? "8자 이상의 비밀번호" : "비밀번호를 입력해 주세요"} required minLength={joining ? 8 : undefined} aria-describedby={joining ? "password-hint" : undefined} /><button type="button" onClick={() => setVisible(!visible)} aria-label={visible ? "비밀번호 숨기기" : "비밀번호 표시"} aria-pressed={visible}>{visible ? "숨기기" : "보기"}</button></div>
              {joining && <p className="hint" id="password-hint">비밀번호는 8자 이상으로 설정해 주세요.</p>}
            </div>
            {joining && <div className="field"><label htmlFor="confirm-password">비밀번호 확인</label><input id="confirm-password" name="confirmPassword" type={visible ? "text" : "password"} autoComplete="new-password" placeholder="비밀번호를 한 번 더 입력해 주세요" required /></div>}
            <div className="form-message" role="status" aria-live="polite">{message}</div>
            <button className="submit-button" type="submit" disabled={pending}>{pending ? "처리 중…" : joining ? "회원가입" : "로그인"}<span aria-hidden="true">→</span></button>
            </fieldset>
          </form>
          <p className="switch-prompt">{joining ? "이미 티켓온 계정이 있나요?" : "아직 티켓온 회원이 아니신가요?"} <Link href={joining ? "/login" : "/signup"}>{joining ? "로그인" : "회원가입"}</Link></p>
          <p className="form-footer">◇ &nbsp; 나의 다음 즐거움이 시작되는 곳, TICKET ON</p>
        </section>
      </main>
      <footer className="site-footer"><span>© TICKET ON</span><span>당신의 모든 설레는 순간과 함께.</span></footer>
    </div>
  );
}
