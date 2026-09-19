const API_URL =
  process.env.NEXT_PUBLIC_API_URL ??
  (typeof window === "undefined"
    ? (process.env.INTERNAL_BACKEND_URL ?? "http://localhost:8080")
    : "");

export interface Schedule {
  id: number;
  date: string;
  time: string;
}

export interface Performance {
  id: number;
  slug: string;
  title: string;
  subtitle: string;
  category: string;
  venue: string;
  date: string;
  price: number;
  color: string;
  tag: string;
  symbol: string;
  schedules: Schedule[];
}

export interface SeatInfo {
  id: number;
  seatNumber: string;
  status: "AVAILABLE" | "HOLD" | "RESERVED";
  available: boolean;
}

export const won = (amount: number) => `${amount.toLocaleString("ko-KR")}원`;

export async function fetchPerformances(): Promise<Performance[]> {
  try {
    const res = await fetch(`${API_URL}/api/performances`, {
      next: { revalidate: 60 },
    });
    if (!res.ok) return [];
    return await res.json();
  } catch (error) {
    console.error("fetchPerformances failed:", error);
    return [];
  }
}

export async function fetchPerformance(slug: string): Promise<Performance | null> {
  try {
    const res = await fetch(`${API_URL}/api/performances/${slug}`, {
      next: { revalidate: 60 },
    });
    if (!res.ok) return null;
    return await res.json();
  } catch (error) {
    console.error("fetchPerformance failed:", error);
    return null;
  }
}

export async function fetchSeats(scheduleId: number): Promise<SeatInfo[]> {
  try {
    const res = await fetch(`${API_URL}/api/schedules/${scheduleId}/seats`, {
      cache: "no-store",
    });
    if (!res.ok) return [];
    return await res.json();
  } catch (error) {
    console.error("fetchSeats failed:", error);
    return [];
  }
}
