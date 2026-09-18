// Browser requests: Spring owns the HttpOnly session cookie.
const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = "ApiError";
  }
}

async function send(path: string, init: RequestInit): Promise<Response> {
  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      ...init, credentials: "include", cache: "no-store",
    });
  } catch {
    throw new Error("서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.");
  }
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new ApiError(response.status,
      typeof error?.message === "string" ? error.message : "요청에 실패했어요. 다시 시도해 주세요.");
  }
  return response;
}

async function post(path: string, body?: unknown): Promise<Response> {
  // Obtain a current token, including after login/logout. Never retry mutations automatically.
  const response = await send("/api/csrf", { method: "GET" });
  const csrf: { headerName: string; token: string } = await response.json();
  const form = body instanceof URLSearchParams;
  return send(path, {
    method: "POST",
    headers: {
      [csrf.headerName]: csrf.token,
      ...(body === undefined ? {} : {
        "Content-Type": form ? "application/x-www-form-urlencoded" : "application/json",
      }),
    },
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
