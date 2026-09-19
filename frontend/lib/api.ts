// Browser requests: Spring owns the HttpOnly session cookie.
const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = "ApiError";
  }
}

// 브라우저 쿠키에서 XSRF-TOKEN 값 추출
function getCookie(name: string): string | undefined {
  if (typeof document === "undefined") return undefined;
  const match = document.cookie.match(new RegExp(`(^|;\\s*)${name}=([^;]*)`));
  return match ? decodeURIComponent(match[2]) : undefined;
}

async function send(path: string, init: RequestInit): Promise<Response> {
  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      ...init,
      credentials: "include",
      cache: "no-store",
    });
  } catch {
    throw new Error("서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.");
  }
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new ApiError(
      response.status,
      typeof error?.message === "string" ? error.message : "요청에 실패했어요. 다시 시도해 주세요."
    );
  }
  return response;
}

async function post(path: string, body?: unknown): Promise<Response> {
  // 별도의 GET /api/csrf 왕복 통신 없이 쿠키에서 바로 읽어 헤더로 전송
  const xsrfToken = getCookie("XSRF-TOKEN");
  const form = body instanceof URLSearchParams;

  const headers: Record<string, string> = {
    ...(xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : {}),
  };

  if (body !== undefined) {
    headers["Content-Type"] = form
      ? "application/x-www-form-urlencoded"
      : "application/json";
  }

  return send(path, {
    method: "POST",
    headers,
    body: body === undefined ? undefined : form ? body.toString() : JSON.stringify(body),
  });
}

export const api = { get: (path: string) => send(path, { method: "GET" }), post };
export type CurrentMember = { id: number; name: string; email: string };

export async function currentMember(): Promise<CurrentMember | null> {
  try {
    const response = await api.get("/api/auth/me");
    return await response.json();
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    throw error;
  }
}
