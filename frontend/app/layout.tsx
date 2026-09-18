import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "TICKET ON | 로그인 · 회원가입",
  description: "기다려온 공연과 만나는 순간, 티켓온.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="ko"
    >
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
