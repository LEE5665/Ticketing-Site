"use client";

import Link from "next/link";
import { useState } from "react";
import { performances, won } from "@/lib/performances";
import AccountMenu from "@/components/account-menu";

export default function TicketHome() {
  const [category, setCategory] = useState("전체");
  const [search, setSearch] = useState("");
  const filtered = performances.filter((show) => (category === "전체" || show.category === category) && `${show.title} ${show.venue}`.includes(search.trim()));
  return <div className="home-page">
    <header className="site-header"><Link className="brand" href="/" aria-label="티켓온 홈"><span className="brand-mark" aria-hidden="true">t</span>TICKET <span>ON</span><i /></Link><AccountMenu /></header>
    <main className="catalog-main">
      <section className="home-hero">
        <div className="hero-copy"><span className="eyebrow">MAKE YOUR NEXT MOMENT</span><h1>일상에 설렘을.<br />당신의 다음 티켓.</h1><p>좋아하는 아티스트, 기다려온 무대.<br />기억하고 싶은 순간을 티켓온에서 만나보세요.</p><a className="hero-link" href="#performances">공연 둘러보기 <span>↗</span></a><small>CONCERT · MUSICAL · FESTIVAL · EXHIBITION</small></div>
        <div className="hero-art" aria-hidden="true"><span className="hero-orbit" /><span className="hero-spark">✳</span><div className="hero-pass"><span>TICKET ON — ADMIT ONE</span><strong>GOOD<br />TIMES<br /><em>AHEAD.</em></strong><div className="pass-bottom">당신을 위한 한 자리 <span>↗</span></div></div><span className="hero-sticker">LIVE THE MOMENT</span></div>
      </section>
      <section className="catalog-section" id="performances" aria-labelledby="catalog-title">
        <div className="catalog-heading"><div><span className="eyebrow">FIND YOUR FAVORITE</span><h2 id="catalog-title">어떤 순간을 만나볼까요?</h2></div><label className="search-box"><span className="sr-only">공연명 또는 장소 검색</span><span aria-hidden="true">⌕</span><input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="공연명 또는 장소 검색" /></label></div>
        <div className="category-filters" aria-label="공연 분류">{["전체", "콘서트", "뮤지컬", "페스티벌", "전시"].map((item) => <button key={item} onClick={() => setCategory(item)} aria-pressed={item === category}>{item}</button>)}</div>
        <div className="catalog-note"><span role="status">총 {filtered.length}개의 공연</span><span>예시 공연 · 예약 체험용</span></div>
        <div className="show-grid">{filtered.map((show) => <Link key={show.id} href={`/performances/${show.id}`} className="show-card"><div className={`show-poster ${show.color}`}><span className="poster-label">TICKET ON ORIGINAL</span><strong>{show.subtitle}</strong><span className="poster-symbol" aria-hidden="true">{show.symbol}</span><span className="poster-caption">{show.tag}</span><span className="poster-arrow" aria-hidden="true">↗</span></div><div className="show-info"><span className="show-category">{show.category}</span><h3>{show.title}</h3><p>{show.venue}</p><p>{show.date}</p><strong>{won(show.price)} <small>부터</small></strong></div></Link>)}</div>
        {filtered.length === 0 && <div className="empty-state"><h3>검색한 공연이 없어요.</h3><p>다른 검색어나 분류로 찾아보세요.</p><button onClick={() => { setSearch(""); setCategory("전체"); }}>전체 공연 보기</button></div>}
      </section>
      <section className="member-banner"><div><span className="eyebrow">YOUR SEAT IS WAITING</span><h2>좋아하는 순간을 놓치지 않도록.</h2><p>티켓온과 함께 다음 설렘을 준비하세요.</p></div><Link href="/signup">회원가입 하기 ↗</Link></section>
    </main>
    <footer className="site-footer"><span>© TICKET ON</span><span>당신의 모든 설레는 순간과 함께.</span></footer>
  </div>;
}
