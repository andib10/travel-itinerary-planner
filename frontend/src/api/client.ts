import type { AuthResponse, CurrentUser, LoginRequest, RegisterRequest } from "@/types/auth"

const BASE_URL = "http://localhost:8080"

/** Thrown on any non-2xx response; `message` is the backend's error text when present. */
export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  token?: string | null
): Promise<T> {
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...options.headers,
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers })
  const raw = await response.text()
  if (!response.ok) {
    let message = raw
    try {
      const parsed = JSON.parse(raw)
      if (typeof parsed?.error === "string") message = parsed.error
    } catch {
      // Not JSON - fall back to raw text.
    }
    throw new ApiError(response.status, message)
  }
  // Some endpoints return 200 with an empty body.
  return raw ? JSON.parse(raw) : (undefined as T)
}

export function login(email: string, password: string) {
  const body: LoginRequest = { email, password }
  return apiFetch<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(body),
  })
}

export function register(email: string, password: string) {
  const body: RegisterRequest = { email, password }
  return apiFetch<AuthResponse>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(body),
  })
}

export function me(token: string) {
  return apiFetch<CurrentUser>("/api/me", {}, token)
}

// No token: verify-email is a public endpoint.
export function verifyEmail(token: string) {
  return apiFetch<{ message: string }>(`/api/auth/verify-email?token=${encodeURIComponent(token)}`)
}

// Same generic response whether or not the email exists.
export function forgotPassword(email: string) {
  return apiFetch<{ message: string }>("/api/auth/forgot-password", {
    method: "POST",
    body: JSON.stringify({ email }),
  })
}

export function resetPassword(token: string, newPassword: string) {
  return apiFetch<{ message: string }>("/api/auth/reset-password", {
    method: "POST",
    body: JSON.stringify({ token, newPassword }),
  })
}

export function changePassword(currentPassword: string, newPassword: string, token: string) {
  return apiFetch<void>(
    "/api/me/password",
    { method: "PUT", body: JSON.stringify({ currentPassword, newPassword }) },
    token
  )
}
