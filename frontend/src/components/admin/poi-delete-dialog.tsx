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
import type { AdminPoiResponse } from "@/types/admin"

type PoiDeleteDialogProps = {
  /** The POI pending deletion, or null when the dialog is closed. */
  poi: AdminPoiResponse | null
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Resolve on success (closes dialog) or reject with an Error to show inline. */
  onConfirm: (poiId: number) => Promise<void>
}

export function PoiDeleteDialog({ poi, open, onOpenChange, onConfirm }: PoiDeleteDialogProps) {
  const [isDeleting, setIsDeleting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Reset transient state whenever the target POI changes or dialog reopens.
  useEffect(() => {
    if (open) {
      setError(null)
      setIsDeleting(false)
    }
  }, [open, poi])

  async function handleConfirm() {
    if (!poi || isDeleting) return
    setIsDeleting(true)
    setError(null)
    try {
      await onConfirm(poi.id)
      onOpenChange(false)
    } catch (err) {
      // Keep the dialog open and show why the deletion was blocked.
      setError(err instanceof Error ? err.message : "Unable to delete this point of interest.")
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Delete point of interest?</AlertDialogTitle>
          <AlertDialogDescription>
            {poi
              ? `"${poi.name}" will be permanently removed. This action cannot be undone.`
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
