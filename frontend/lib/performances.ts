// Sample catalog for the frontend preview. Replace with the Spring API later.
export const performances = [
  { id: "blue-hour", title: "블루 아워 라이브", subtitle: "BLUE HOUR", category: "콘서트", venue: "서울 올림픽홀", date: "2026.11.07 – 11.08", dates: ["2026-11-07", "2026-11-08"], price: 99000, color: "blue", tag: "가을의 밤을 채울 목소리", symbol: "◒" },
  { id: "midnight", title: "미드나잇 익스프레스", subtitle: "MIDNIGHT\nEXPRESS", category: "뮤지컬", venue: "서울 아트씨어터", date: "2026.11.14 – 11.15", dates: ["2026-11-14", "2026-11-15"], price: 85000, color: "purple", tag: "마지막 기차에서 시작된 이야기", symbol: "✦" },
  { id: "green-days", title: "그린 데이즈 페스티벌", subtitle: "GREEN\nDAYS", category: "페스티벌", venue: "한강 잔디공원", date: "2026.11.21 – 11.22", dates: ["2026-11-21", "2026-11-22"], price: 110000, color: "green", tag: "음악과 함께하는 느긋한 하루", symbol: "✳" },
  { id: "room-of-light", title: "빛의 방", subtitle: "ROOM\nOF LIGHT", category: "전시", venue: "성수 아트스페이스", date: "2026.11.28 – 11.29", dates: ["2026-11-28", "2026-11-29"], price: 22000, color: "orange", tag: "빛과 공간 사이, 새로운 감각", symbol: "◐" },
] as const;

export type Performance = (typeof performances)[number];
export const won = (amount: number) => `${amount.toLocaleString("ko-KR")}원`;
