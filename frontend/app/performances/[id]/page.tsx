import { notFound } from "next/navigation";
import Booking from "@/components/booking";
import { performances } from "@/lib/performances";

export function generateStaticParams() {
  return performances.map(({ id }) => ({ id }));
}

export default async function PerformancePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const performance = performances.find((show) => show.id === id);
  if (!performance) notFound();
  return <Booking performance={performance} />;
}
