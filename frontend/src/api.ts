let csrf: { token: string; headerName: string } | null = null;
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
  }
}
export async function renewCsrf() {
  const res = await fetch("/api/auth/csrf", { credentials: "same-origin" });
  if (!res.ok) throw new ApiError("Cannot reach the server", res.status);
  csrf = await res.json();
}
export async function api<T>(
  path: string,
  method = "GET",
  body?: unknown,
): Promise<T> {
  const headers: Record<string, string> = {};
  if (method !== "GET") {
    if (!csrf) await renewCsrf();
    headers[csrf!.headerName] = csrf!.token;
  }
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const res = await fetch("/api" + path, {
    method,
    headers,
    credentials: "same-origin",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!res.ok) {
    if (res.status === 401 || res.status === 403) csrf = null;
    let message = "Request failed";
    try {
      const data = await res.json();
      message = data.detail || data.message || data.error || message;
    } catch {}
    throw new ApiError(message, res.status);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}
export async function login(username: string, password: string) {
  await renewCsrf();
  const res = await fetch("/api/auth/login", {
    method: "POST",
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      [csrf!.headerName]: csrf!.token,
    },
    body: new URLSearchParams({ username, password }),
  });
  if (!res.ok)
    throw new ApiError("Check your username and password.", res.status);
  await renewCsrf();
}
export async function logout() {
  await api("/auth/logout", "POST");
  csrf = null;
}
