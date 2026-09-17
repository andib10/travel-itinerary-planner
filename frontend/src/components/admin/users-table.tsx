import { useEffect, useState } from "react"

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { TablePagination } from "@/components/admin/table-pagination"
import { UserRoleDialog } from "@/components/admin/user-role-dialog"
import { UserDeleteDialog } from "@/components/admin/user-delete-dialog"
import { usePagination } from "@/hooks/usePagination"
import { cn } from "@/lib/utils"
import { useAuth } from "@/context/AuthContext"
import { getAdminUsers, updateUserRole, deleteUser } from "@/api/admin"
import { ApiError } from "@/api/client"
import type { AdminUserResponse } from "@/types/admin"

// Falls back to a plain badge for an unexpected role value instead of crashing.
const ROLE_CONFIG: Record<string, { label: string; className: string }> = {
  ADMIN: { label: "Admin", className: "bg-primary/15 text-primary" },
  USER: { label: "User", className: "bg-muted text-muted-foreground" },
}
const FALLBACK_ROLE_CONFIG = { label: "Unknown", className: "bg-muted text-muted-foreground" }

function formatJoinedDate(isoDateTime: string): string {
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(new Date(isoDateTime))
}

export function UsersTable() {
  const { token, user: currentUser } = useAuth()
  const [users, setUsers] = useState<AdminUserResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [changingRole, setChangingRole] = useState<AdminUserResponse | null>(null)
  const [deleting, setDeleting] = useState<AdminUserResponse | null>(null)

  useEffect(() => {
    if (!token) return
    setError(null)
    getAdminUsers(token)
      .then(setUsers)
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : "Failed to load users")
      })
  }, [token])

  // Must run before the early returns below (Rules of Hooks).
  const { page, setPage, totalPages, pageItems } = usePagination(users ?? [], 10)

  async function handleRoleChange(userId: number, role: string) {
    if (!token) return
    const updated = await updateUserRole(userId, role, token)
    setUsers((current) => current?.map((u) => (u.id === userId ? updated : u)) ?? null)
  }

  async function handleDelete(userId: number) {
    if (!token) return
    await deleteUser(userId, token)
    setUsers((current) => current?.filter((u) => u.id !== userId) ?? null)
  }

  if (error) return <p className="text-sm text-destructive">{error}</p>
  if (users === null) return <p className="text-sm text-muted-foreground">Loading users…</p>

  return (
    <div className="flex flex-col gap-4">
      <div className="overflow-hidden rounded-xl ring-1 ring-foreground/10">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/50">
              <TableHead>Email</TableHead>
              <TableHead>Role</TableHead>
              <TableHead className="text-right">Trips</TableHead>
              <TableHead className="text-right">Joined</TableHead>
              <TableHead className="w-0 text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {users.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="h-24 text-center text-muted-foreground">
                  No users yet.
                </TableCell>
              </TableRow>
            ) : (
              pageItems.map((user) => {
                const { label, className } = ROLE_CONFIG[user.role] ?? FALLBACK_ROLE_CONFIG
                // Also blocked on the backend; hidden here to avoid a confusing disabled button.
                const isSelf = user.id === currentUser?.id
                return (
                  <TableRow key={user.id}>
                    <TableCell className="font-medium text-foreground">{user.email}</TableCell>
                    <TableCell>
                      <Badge className={cn(className)}>{label}</Badge>
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{user.tripCount}</TableCell>
                    <TableCell className="text-right text-muted-foreground">
                      {formatJoinedDate(user.createdAt)}
                    </TableCell>
                    <TableCell className="text-right">
                      {isSelf ? (
                        <span className="text-xs text-muted-foreground">You</span>
                      ) : (
                        <div className="flex justify-end gap-1">
                          <Button variant="ghost" size="sm" onClick={() => setChangingRole(user)}>
                            {user.role === "ADMIN" ? "Remove admin" : "Make admin"}
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                            onClick={() => setDeleting(user)}
                          >
                            Delete
                          </Button>
                        </div>
                      )}
                    </TableCell>
                  </TableRow>
                )
              })
            )}
          </TableBody>
        </Table>
      </div>

      <TablePagination page={page} totalPages={totalPages} onPageChange={setPage} />

      <UserRoleDialog
        user={changingRole}
        open={changingRole !== null}
        onOpenChange={(open) => {
          if (!open) setChangingRole(null)
        }}
        onConfirm={handleRoleChange}
      />

      <UserDeleteDialog
        user={deleting}
        open={deleting !== null}
        onOpenChange={(open) => {
          if (!open) setDeleting(null)
        }}
        onConfirm={handleDelete}
      />
    </div>
  )
}
