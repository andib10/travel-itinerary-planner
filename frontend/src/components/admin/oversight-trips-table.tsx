import { useEffect, useState } from "react"
import { Link } from "react-router-dom"

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { TripStatusBadge } from "@/components/trip-status-badge"
import { TripDeleteDialog } from "@/components/admin/trip-delete-dialog"
import { TablePagination } from "@/components/admin/table-pagination"
import { usePagination } from "@/hooks/usePagination"
import { useAuth } from "@/context/AuthContext"
import { getAdminTrips, deleteAdminTrip } from "@/api/admin"
import { ApiError } from "@/api/client"
import type { AdminTripResponse } from "@/types/admin"

export function OversightTripsTable() {
  const { token } = useAuth()
  const [trips, setTrips] = useState<AdminTripResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState<AdminTripResponse | null>(null)

  useEffect(() => {
    if (!token) return
    setError(null)
    getAdminTrips(token)
      .then(setTrips)
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : "Failed to load trips")
      })
  }, [token])

  async function handleDelete(tripId: number) {
    if (!token) return
    await deleteAdminTrip(tripId, token)
    setTrips((current) => current?.filter((trip) => trip.id !== tripId) ?? null)
  }

  // Must run before the early returns below (Rules of Hooks).
  const { page, setPage, totalPages, pageItems } = usePagination(trips ?? [], 10)

  if (error) return <p className="text-sm text-destructive">{error}</p>
  if (trips === null) return <p className="text-sm text-muted-foreground">Loading trips…</p>

  return (
    <div className="flex flex-col gap-4">
      <div className="overflow-hidden rounded-xl ring-1 ring-foreground/10">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/50">
              <TableHead>Destination</TableHead>
              <TableHead>Owner</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Days</TableHead>
              <TableHead className="w-0 text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {trips.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="h-24 text-center text-muted-foreground">
                  No trips yet.
                </TableCell>
              </TableRow>
            ) : (
              pageItems.map((trip) => (
                <TableRow key={trip.id}>
                  <TableCell className="font-medium text-foreground">{trip.destination}</TableCell>
                  <TableCell className="text-muted-foreground">{trip.ownerEmail}</TableCell>
                  <TableCell>
                    <TripStatusBadge status={trip.status} />
                  </TableCell>
                  <TableCell className="text-right tabular-nums">{trip.dayCount}</TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      {/* Admin detail route, bypasses ownership check. */}
                      <Button
                        variant="outline"
                        size="sm"
                        nativeButton={false}
                        render={<Link to={`/admin/trips/${trip.id}`} />}
                      >
                        View
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                        onClick={() => setDeleting(trip)}
                      >
                        Delete
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <TablePagination page={page} totalPages={totalPages} onPageChange={setPage} />

      <TripDeleteDialog
        trip={deleting}
        open={deleting !== null}
        onOpenChange={(open) => {
          if (!open) setDeleting(null)
        }}
        onConfirm={handleDelete}
      />
    </div>
  )
}
