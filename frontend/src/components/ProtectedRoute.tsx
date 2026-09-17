import type { ReactNode } from "react"
import { Navigate } from "react-router-dom"
import { useAuth } from "@/context/AuthContext"

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, isLoading } = useAuth()

  // Avoid redirecting before session restore from sessionStorage finishes.
  if (isLoading) return null

  if (!user) return <Navigate to="/login" replace />

  return <>{children}</>
}
