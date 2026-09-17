export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
}

export interface AuthResponse {
  token: string
}

export interface CurrentUser {
  id: number
  email: string
  role: string
  emailVerified: boolean
}
