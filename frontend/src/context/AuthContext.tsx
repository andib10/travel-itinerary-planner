import { createContext, useContext, useEffect, useState, type ReactNode } from "react"
import * as api from "@/api/client"
import type { CurrentUser } from "@/types/auth"

const TOKEN_STORAGE_KEY = "authToken"

interface AuthContextValue {
  user: CurrentUser | null
  token: string | null
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => void
  isLoading: boolean
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null)
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // Restore an existing session on load instead of forcing a re-login.
  useEffect(() => {
    const storedToken = sessionStorage.getItem(TOKEN_STORAGE_KEY)
    if (!storedToken) {
      setIsLoading(false)
      return
    }
    api
      .me(storedToken)
      .then((currentUser) => {
        setToken(storedToken)
        setUser(currentUser)
      })
      .catch(() => {
        // Token invalid/expired - drop it.
        sessionStorage.removeItem(TOKEN_STORAGE_KEY)
      })
      .finally(() => setIsLoading(false))
  }, [])

  async function login(email: string, password: string) {
    const { token: newToken } = await api.login(email, password)
    const currentUser = await api.me(newToken)
    sessionStorage.setItem(TOKEN_STORAGE_KEY, newToken)
    setToken(newToken)
    setUser(currentUser)
  }

  async function register(email: string, password: string) {
    const { token: newToken } = await api.register(email, password)
    const currentUser = await api.me(newToken)
    sessionStorage.setItem(TOKEN_STORAGE_KEY, newToken)
    setToken(newToken)
    setUser(currentUser)
  }

  function logout() {
    sessionStorage.removeItem(TOKEN_STORAGE_KEY)
    setToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider")
  }
  return context
}
