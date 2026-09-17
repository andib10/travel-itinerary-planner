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
import type { AdminTripResponse } from "@/types/admin"

type TripDeleteDialogProps = {
  /** The trip pending deletion, or null when the dialog is closed. */
  trip: AdminTripResponse | null
  open: boolean
  onOpenChange: (open: boolean) => void
  /**
   * Attempts deletion. Resolve for success (dialog closes) or reject with an
   * Error whose message is shown inline so the dialog stays open.
   */
  onConfirm: (tripId: number) => Promise<void>
}

export function TripDeleteDialog({ trip, open, onOpenChange, onConfirm }: TripDeleteDialogProps) {
  const [isDeleting, setIsDeleting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Reset transient state whenever the target trip changes or dialog reopens.
  useEffect(() => {
    if (open) {
      setError(null)
      setIsDeleting(false)
    }
  }, [open, trip])

  async function handleConfirm() {
    if (!trip || isDeleting) return
    setIsDeleting(true)
    setError(null)
    try {
      await onConfirm(trip.id)
      onOpenChange(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to delete this trip.")
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Delete trip?</AlertDialogTitle>
          <AlertDialogDescription>
            {trip
              ? `"${trip.destination}" (owned by ${trip.ownerEmail}) will be permanently removed, along with its itinerary. This action cannot be undone.`
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
