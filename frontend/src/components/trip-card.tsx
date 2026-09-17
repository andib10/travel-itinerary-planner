import { Link } from "react-router-dom"
import { Trash2 } from "lucide-react"

import { Card, CardHeader, CardTitle } from "@/components/ui/card"
import { TripStatusBadge } from "@/components/trip-status-badge"
import { formatDateRange } from "@/lib/trips"
import type { TripSummaryResponse } from "@/types/trip"

type TripCardProps = {
  trip: TripSummaryResponse
  /** Omit to hide the delete button entirely. */
  onDeleteClick?: (trip: TripSummaryResponse) => void
}

export function TripCard({ trip, onDeleteClick }: TripCardProps) {
  return (
    <Link
      to={`/trips/${trip.id}`}
      className="group rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
    >
      <Card className="h-full gap-3 transition-all group-hover:ring-foreground/25 group-hover:shadow-sm">
        <CardHeader className="gap-2">
          <div className="flex items-start justify-between gap-3">
            <CardTitle className="text-base font-bold text-balance">
              {trip.destination}
            </CardTitle>
            <div className="flex items-center gap-1.5">
              <TripStatusBadge status={trip.status} />
              {onDeleteClick ? (
                <button
                  type="button"
                  aria-label={`Delete ${trip.destination}`}
                  onClick={(event) => {
                    // The whole card is a Link - stop this click from also navigating.
                    event.preventDefault()
                    event.stopPropagation()
                    onDeleteClick(trip)
                  }}
                  className="rounded-md p-1 text-muted-foreground opacity-0 transition-opacity hover:bg-destructive/10 hover:text-destructive focus-visible:opacity-100 focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none group-hover:opacity-100"
                >
                  <Trash2 className="size-4" />
                </button>
              ) : null}
            </div>
          </div>
          <p className="text-sm text-muted-foreground">
            {formatDateRange(trip.startDate, trip.endDate)}
          </p>
        </CardHeader>
      </Card>
    </Link>
  )
}
