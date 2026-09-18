import TicketHome from "@/components/ticket-home";
import type { Metadata } from "next";

export const metadata: Metadata = { title: "TICKET ON | 당신의 다음 순간" };

export default function Home() {
  return <TicketHome />;
}
