import { notFound } from "next/navigation";
import Booking from "@/components/booking";
import { fetchPerformance, fetchPerformances } from "@/lib/performances";

export async function generateStaticParams() {
  const performances = await fetchPerformances();
  return performances.map(({ slug }) => ({ id: slug }));
}

export default async function PerformancePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const performance = await fetchPerformance(id);
  if (!performance) notFound();
  return <Booking performance={performance} />;
}
