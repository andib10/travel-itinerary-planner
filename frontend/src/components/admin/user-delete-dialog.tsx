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

type UserDeleteDialogProps = {
  /** The user pending deletion, or null when the dialog is closed. */
  user: AdminUserResponse | null
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Resolve on success (closes dialog) or reject with an Error to show inline. */
  onConfirm: (userId: number) => Promise<void>
}

export function UserDeleteDialog({ user, open, onOpenChange, onConfirm }: UserDeleteDialogProps) {
  const [isDeleting, setIsDeleting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      setError(null)
      setIsDeleting(false)
    }
  }, [open, user])

  async function handleConfirm() {
    if (!user || isDeleting) return
    setIsDeleting(true)
    setError(null)
    try {
      await onConfirm(user.id)
      onOpenChange(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to delete this user.")
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Delete user?</AlertDialogTitle>
          <AlertDialogDescription>
            {user
              ? `"${user.email}" and all ${user.tripCount} of their trip(s) will be permanently removed. This action cannot be undone.`
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
          <AlertDialogCancel disabled={isDeleting}>Cancel</AlertDialogCancel>
          <AlertDialogAction variant="destructive" onClick={handleConfirm} disabled={isDeleting}>
            {isDeleting ? (
              <>
                <Loader2 className="animate-spin" />
                Deleting...
              </>
            ) : (
              "Delete"
            )}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
