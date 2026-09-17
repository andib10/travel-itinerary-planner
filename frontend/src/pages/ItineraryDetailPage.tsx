import { useEffect, useState } from "react"
import { Link, useParams } from "react-router-dom"
import { ArrowLeft } from "lucide-react"

import { ItineraryDetail } from "@/components/itinerary/itinerary-detail"
import { useAuth } from "@/context/AuthContext"
import { getTripDetail } from "@/api/trips"
import { ApiError } from "@/api/client"
import type { TripDetailResponse } from "@/types/trip"

function ItineraryDetailPage() {
  const { id } = useParams()
  const { token } = useAuth()
  const [trip, setTrip] = useState<TripDetailResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!token || !id) return
    setError(null)
    getTripDetail(Number(id), token)
      .then(setTrip)
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : "Failed to load this trip")
      })
  }, [token, id])

  return (
    <div className="min-h-svh bg-background">
      <header className="w-full border-b border-border/60">
        <nav className="mx-auto flex h-16 max-w-5xl items-center justify-between px-6">
          <Link
            to="/trips"
            className="inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
          >
            <ArrowLeft className="size-4" />
            My Trips
          </Link>
          <Link
            to="/trips"
            className="font-sans text-lg font-medium tracking-tight text-foreground"
          >
            Voyager
          </Link>
        </nav>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-8">
        {error ? (
          <p className="text-sm text-destructive">{error}</p>
        ) : trip === null || token === null ? (
          <p className="text-sm text-muted-foreground">Loading your trip…</p>
        ) : (
          <ItineraryDetail trip={trip} token={token} />
        )}
      </main>
    </div>
  )
}

export default ItineraryDetailPage
