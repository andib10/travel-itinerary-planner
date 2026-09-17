import { MapContainer, Marker, Polyline, Popup, TileLayer } from "react-leaflet"
import L from "leaflet"
import markerIcon2x from "leaflet/dist/images/marker-icon-2x.png"
import markerIcon from "leaflet/dist/images/marker-icon.png"
import markerShadow from "leaflet/dist/images/marker-shadow.png"

import { cn } from "@/lib/utils"
import type { StopResponse } from "@/types/trip"

// Repoint Leaflet's default marker icon paths at the bundled asset URLs (Vite issue).
delete (L.Icon.Default.prototype as unknown as { _getIconUrl?: unknown })._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

/** Rome, as a reasonable fallback center if a day somehow has no stops. */
const FALLBACK_CENTER: [number, number] = [41.9028, 12.4964]

export function ItineraryMap({
  stops,
  dayId,
  className,
}: {
  /** Expected already sorted by orderIndex - draws the route line in this order. */
  stops: StopResponse[]
  /** Forces a remount on day change so the map recenters (center only applies on initial mount). */
  dayId: number | null
  className?: string
}) {
  const positions = stops.map((stop) => [stop.lat, stop.lng] as [number, number])

  if (positions.length === 0) {
    return (
      <div
        role="img"
        aria-label="Map"
        className={cn(
          "flex items-center justify-center rounded-xl bg-muted text-sm font-medium text-muted-foreground ring-1 ring-foreground/10",
          className
        )}
      >
        No stops to show on the map yet.
      </div>
    )
  }

  return (
    <div className={cn("overflow-hidden rounded-xl ring-1 ring-foreground/10", className)}>
      <MapContainer
        key={dayId}
        center={positions[0] ?? FALLBACK_CENTER}
        zoom={14}
        style={{ height: "100%", width: "100%" }}
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        />
        {stops.map((stop) => (
          <Marker key={stop.id} position={[stop.lat, stop.lng]}>
            <Popup>{stop.poiName}</Popup>
          </Marker>
        ))}
        <Polyline positions={positions} />
      </MapContainer>
    </div>
  )
}
