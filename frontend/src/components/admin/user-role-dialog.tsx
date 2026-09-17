import { useEffect, useState } from "react"
import { Loader2, TriangleAlert } from "lucide-react"

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import type { AdminUserResponse } from "@/types/admin"

type UserRoleDialogProps = {
  /** The user whose role is being changed, or null when the dialog is closed. */
  user: AdminUserResponse | null
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Resolve on success (closes dialog) or reject with an Error to show inline. */
  onConfirm: (userId: number, role: string) => Promise<void>
}

export function UserRoleDialog({ user, open, onOpenChange, onConfirm }: UserRoleDialogProps) {
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      setError(null)
      setIsSaving(false)
    }
  }, [open, user])

  const nextRole = user?.role === "ADMIN" ? "USER" : "ADMIN"
  const isPromoting = nextRole === "ADMIN"

  async function handleConfirm() {
    if (!user || isSaving) return
    setIsSaving(true)
    setError(null)
    try {
      await onConfirm(user.id, nextRole)
      onOpenChange(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to update this user's role.")
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            {isPromoting ? "Make admin?" : "Remove admin access?"}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {user
              ? isPromoting
                ? `"${user.email}" will be granted admin access to this app.`
                : `"${user.email}" will lose admin access to this app.`
              : "This action cannot be undone."}
          </AlertDialogDescription>
        </AlertDialogHeader>

        {error ? (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive"
          >
            <TriangleAlert className="mt-0.5 size-4 shrink-0" />
            <span>{error}</span>
          </div>
        ) : null}

        <AlertDialogFooter>
          <AlertDialogCancel disabled={isSaving}>Cancel</AlertDialogCancel>
          <AlertDialogAction onClick={handleConfirm} disabled={isSaving}>
            {isSaving ? (
              <>
                <Loader2 className="animate-spin" />
                Saving...
              </>
            ) : isPromoting ? (
              "Make admin"
            ) : (
              "Remove admin"
            )}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
