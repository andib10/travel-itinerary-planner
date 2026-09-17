import type { ReactNode } from "react"
import { Navigate } from "react-router-dom"
import { useAuth } from "@/context/AuthContext"

// UX convenience only - real protection is the backend's 403 on /api/admin/**.
export function AdminRoute({ children }: { children: ReactNode }) {
  const { user, isLoading } = useAuth()

  if (isLoading) return null

  if (!user) return <Navigate to="/login" replace />

  if (user.role !== "ADMIN") return <Navigate to="/trips" replace />

  return <>{children}</>
}
