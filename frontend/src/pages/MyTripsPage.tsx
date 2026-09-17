import { useEffect, useState } from "react"

import { SiteNav } from "@/components/site-nav"
import { PlanTripButton } from "@/components/plan-trip-button"
import { TripsGrid } from "@/components/trips-grid"
import { EmptyTrips } from "@/components/empty-trips"
import { TripDeleteDialog } from "@/components/trip-delete-dialog"
import { useAuth } from "@/context/AuthContext"
import { getMyTrips, deleteTrip } from "@/api/trips"
import { ApiError } from "@/api/client"
import type { TripSummaryResponse } from "@/types/trip"

function MyTripsPage() {
  const { token } = useAuth()
  const [trips, setTrips] = useState<TripSummaryResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState<TripSummaryResponse | null>(null)

  useEffect(() => {
    if (!token) return
    setError(null)
    getMyTrips(token)
      .then(setTrips)
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : "Failed to load your trips")
      })
  }, [token])

  async function handleDelete(tripId: number) {
    if (!token) return
    await deleteTrip(tripId, token)
    setTrips((current) => current?.filter((trip) => trip.id !== tripId) ?? null)
  }

  return (
    <div className="min-h-svh bg-background">
      <SiteNav />

      <main className="mx-auto max-w-6xl px-6 py-10">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <h1 className="font-sans text-2xl font-semibold tracking-tight text-foreground text-balance">
            My Trips
          </h1>
          {trips && trips.length > 0 && <PlanTripButton />}
        </div>

        <div className="mt-8">
          {error ? (
            <p className="text-sm text-destructive">{error}</p>
          ) : trips === null ? (
            <p className="text-sm text-muted-foreground">Loading your trips…</p>
          ) : trips.length > 0 ? (
            <TripsGrid trips={trips} onDeleteClick={setDeleting} />
          ) : (
            <EmptyTrips />
          )}
        </div>
      </main>

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

export default MyTripsPage
