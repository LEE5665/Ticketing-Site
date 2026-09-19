import TicketHome from "@/components/ticket-home";
import { fetchPerformances } from "@/lib/performances";
import type { Metadata } from "next";

export const metadata: Metadata = { title: "TICKET ON | 당신의 다음 순간" };

export default async function Home() {
  const performances = await fetchPerformances();
  return <TicketHome initialPerformances={performances} />;
}
