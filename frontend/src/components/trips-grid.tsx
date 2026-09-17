import { TripCard } from "@/components/trip-card"
import type { TripSummaryResponse } from "@/types/trip"

type TripsGridProps = {
  trips: TripSummaryResponse[]
  onDeleteClick?: (trip: TripSummaryResponse) => void
}

export function TripsGrid({ trips, onDeleteClick }: TripsGridProps) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {trips.map((trip) => (
        <TripCard key={trip.id} trip={trip} onDeleteClick={onDeleteClick} />
      ))}
    </div>
  )
}
