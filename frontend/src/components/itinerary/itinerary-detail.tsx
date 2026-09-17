import { useEffect, useState } from "react"
import { arrayMove } from "@dnd-kit/sortable"
import { Plus } from "lucide-react"

import { DayTabs } from "@/components/itinerary/day-tabs"
import { FixScheduleBanner } from "@/components/itinerary/fix-schedule-banner"
import { ItineraryMap } from "@/components/itinerary/itinerary-map"
import { StopList } from "@/components/itinerary/stop-list"
import { AddStopDialog } from "@/components/itinerary/add-stop-dialog"
import { Button } from "@/components/ui/button"
import { addStop, deleteStop, getTripDetail, optimizeDay, reorderStop } from "@/api/trips"
import { formatDateRange } from "@/lib/trips"
import type { ItineraryDayResponse, StopResponse, SuggestedStopResponse, TripDetailResponse } from "@/types/trip"

// Mirrors the backend's ItineraryOptimizer gap threshold/haversine calc; only used
// to decide whether to offer a fix, the actual fix is always computed server-side.
const MAX_IDLE_GAP_MINUTES = 90
const EARTH_RADIUS_METERS = 6371000
const WALKING_SPEED_KMH = 5.0

function haversineMeters(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const toRad = (deg: number) => (deg * Math.PI) / 180
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2
  return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function travelMinutes(from: StopResponse, to: StopResponse): number {
  const distanceKm = haversineMeters(from.lat, from.lng, to.lat, to.lng) / 1000
  return Math.ceil((distanceKm / WALKING_SPEED_KMH) * 60)
}

function toMinutesSinceMidnight(time: string): number {
  const [h, m] = time.split(":").map(Number)
  return h * 60 + m
}

// Returns the largest gap between consecutive stops exceeding the threshold, or null.
function findLargestExcessiveGap(stops: StopResponse[]): number | null {
  const sorted = [...stops].sort((a, b) => a.orderIndex - b.orderIndex)
  let largest: number | null = null
  for (let i = 1; i < sorted.length; i++) {
    const previous = sorted[i - 1]
    const stop = sorted[i]
    const requiredTravel = travelMinutes(previous, stop)
    const earliestPermissibleStart = toMinutesSinceMidnight(previous.endTime) + requiredTravel
    const gapMinutes = toMinutesSinceMidnight(stop.startTime) - earliestPermissibleStart
    if (gapMinutes > MAX_IDLE_GAP_MINUTES && (largest === null || gapMinutes > largest)) {
      largest = gapMinutes
    }
  }
  return largest
}

function formatGapDuration(totalMinutes: number): string {
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`
}

// Extracts the user-facing part of any "unresolved" optimizer detail lines.
function buildUnresolvedWarning(details: string[]): string | null {
  const messages = details
    .filter((line) => line.includes("unresolved"))
    .map((line) => line.split(" — ")[0])
  return messages.length > 0 ? messages.join(" ") : null
}

export function ItineraryDetail({
  trip,
  token,
  readOnly = false,
}: {
  trip: TripDetailResponse
  token: string
  /** Admin oversight view - stop list is display-only, no reorder/remove/fix. */
  readOnly?: boolean
}) {
  // Local mutable copy of days/stops; mutations reconcile with the backend afterward.
  const [days, setDays] = useState<ItineraryDayResponse[]>(trip.days)
  const [activeDayId, setActiveDayId] = useState<number | null>(trip.days[0]?.id ?? null)
  // "Times may need updating" banner - stays hidden until a reorder happens.
  const [bannerVisible, setBannerVisible] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [isOptimizing, setIsOptimizing] = useState(false)
  // Stop id most recently dragged; its time is stale so "Fix schedule" recomputes it.
  const [lastMovedStopId, setLastMovedStopId] = useState<number | null>(null)
  // Set when Fix schedule couldn't resolve everything (e.g. opening-hours conflict).
  const [unresolvedWarning, setUnresolvedWarning] = useState<string | null>(null)
  // Set when a reorder/delete leaves an excessive gap; offered, never auto-applied.
  const [gapOfferMinutes, setGapOfferMinutes] = useState<number | null>(null)
  const [isClosingGap, setIsClosingGap] = useState(false)
  const [addStopDialogOpen, setAddStopDialogOpen] = useState(false)

  // Keep the active tab valid if the previously-selected day stops existing.
  useEffect(() => {
    if (!days.some((day) => day.id === activeDayId)) {
      setActiveDayId(days[0]?.id ?? null)
    }
  }, [days, activeDayId])

  const activeDay = days.find((day) => day.id === activeDayId) ?? days[0]
  const sortedStops = [...(activeDay?.stops ?? [])].sort((a, b) => a.orderIndex - b.orderIndex)

  function handleReorder(stopId: number, newIndex: number) {
    if (readOnly) return
    const targetDayId = activeDay?.id
    if (targetDayId === undefined) return

    // Optimistic update: move the stop and renumber the whole day 1..N, matching the backend.
    setDays((prev) =>
      prev.map((day) => {
        if (day.id !== targetDayId) return day
        const currentSorted = [...day.stops].sort((a, b) => a.orderIndex - b.orderIndex)
        const oldIndex = currentSorted.findIndex((stop) => stop.id === stopId)
        if (oldIndex === -1) return day
        const reordered = arrayMove(currentSorted, oldIndex, newIndex)
        return { ...day, stops: reordered.map((stop, i) => ({ ...stop, orderIndex: i + 1 })) }
      })
    )
    setBannerVisible(true)
    setActionError(null)
    setLastMovedStopId(stopId)
    setUnresolvedWarning(null) // fresh reorder supersedes the last fix result
    // Gap check runs once the Fix schedule decision is made, not here, to avoid duplicate banners.
    setGapOfferMinutes(null)

    // 1-based to match the backend's orderIndex convention.
    reorderStop(stopId, newIndex + 1, token)
      .then(() => getTripDetail(trip.id, token))
      .then((fresh) => setDays(fresh.days))
      .catch((err) => {
        console.error("Failed to persist stop reorder:", err)
        setActionError("Couldn't save the new order - please try again.")
      })
  }

  function handleRemoveStop(stopId: number) {
    if (readOnly) return
    const targetDayId = activeDay?.id
    if (targetDayId === undefined) return

    setActionError(null)

    // Removed only after the backend confirms it; no refetch needed, unlike reorder.
    deleteStop(stopId, token)
      .then(() => {
        let remainingStops: StopResponse[] = []
        setDays((prev) =>
          prev.map((day) => {
            if (day.id !== targetDayId) return day
            remainingStops = day.stops.filter((stop) => stop.id !== stopId)
            return { ...day, stops: remainingStops }
          })
        )
        setGapOfferMinutes(findLargestExcessiveGap(remainingStops))
      })
      .catch((err) => {
        console.error("Failed to remove stop:", err)
        setActionError("Couldn't remove that stop - please try again.")
      })
  }

  function handleCloseGap() {
    if (readOnly) return
    const dayId = activeDay?.id
    if (dayId === undefined) return

    setIsClosingGap(true)
    setActionError(null)

    // Also passes lastMovedStopId in case that same reorder left its time stale too.
    optimizeDay(trip.id, dayId, token, lastMovedStopId ?? undefined, true)
      .then(({ day: optimizedDay }) => {
        setDays((prev) => prev.map((day) => (day.id === dayId ? optimizedDay : day)))
        setGapOfferMinutes(null)
        setLastMovedStopId(null)
        setBannerVisible(false)
      })
      .catch((err) => {
        console.error("Failed to close the gap:", err)
        setActionError("Couldn't adjust the schedule - please try again.")
      })
      .finally(() => setIsClosingGap(false))
  }

  function handleFixSchedule() {
    if (readOnly) return
    const dayId = activeDay?.id
    if (dayId === undefined) return

    setIsOptimizing(true)
    setActionError(null)

    optimizeDay(trip.id, dayId, token, lastMovedStopId ?? undefined)
      .then(({ day: optimizedDay, optimization }) => {
        setDays((prev) => prev.map((day) => (day.id === dayId ? optimizedDay : day)))
        setBannerVisible(false)
        setLastMovedStopId(null)
        setUnresolvedWarning(buildUnresolvedWarning(optimization.details))
        // Keep the gap offer accurate after the schedule reset.
        setGapOfferMinutes(findLargestExcessiveGap(optimizedDay.stops))
      })
      .catch((err) => {
        console.error("Failed to optimize day:", err)
        setActionError("Couldn't fix the schedule - please try again.")
      })
      .finally(() => setIsOptimizing(false))
  }

  // Adds the stop with a placeholder time, then reuses Fix schedule to give it a real one.
  async function handleAddStop(poi: SuggestedStopResponse) {
    if (readOnly) return
    const dayId = activeDay?.id
    if (dayId === undefined) return

    const newStop = await addStop(dayId, poi.id, sortedStops.length + 1, token)
    const { day: optimizedDay, optimization } = await optimizeDay(trip.id, dayId, token, newStop.id)
    setDays((prev) => prev.map((day) => (day.id === dayId ? optimizedDay : day)))
    setUnresolvedWarning(buildUnresolvedWarning(optimization.details))
  }

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="font-sans text-3xl font-semibold tracking-tight text-foreground text-balance">
          {trip.destination}
        </h1>
        <p className="text-sm text-muted-foreground">
          {formatDateRange(trip.startDate, trip.endDate)}
        </p>
      </header>

      <DayTabs
        days={days}
        activeDayId={activeDay?.id ?? null}
        onSelectDay={setActiveDayId}
      />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <div className="flex flex-col gap-3">
          {!readOnly && bannerVisible ? (
            <FixScheduleBanner
              onFix={handleFixSchedule}
              onDismiss={() => {
                setBannerVisible(false)
                setLastMovedStopId(null)
                // Now that Fix schedule has been dismissed, check for a gap.
                setGapOfferMinutes(activeDay ? findLargestExcessiveGap(activeDay.stops) : null)
              }}
              fixing={isOptimizing}
            />
          ) : null}
          {actionError ? <p className="text-sm text-destructive">{actionError}</p> : null}
          {!readOnly && unresolvedWarning ? (
            <FixScheduleBanner
              message={unresolvedWarning}
              onDismiss={() => setUnresolvedWarning(null)}
            />
          ) : null}
          {!readOnly && gapOfferMinutes !== null ? (
            <FixScheduleBanner
              message={`There's a ${formatGapDuration(gapOfferMinutes)} gap in this day now. Adjust the schedule to close it?`}
              onFix={handleCloseGap}
              onDismiss={() => setGapOfferMinutes(null)}
              fixLabel="Close gap"
              fixingLabel="Closing…"
              fixing={isClosingGap}
            />
          ) : null}

          <StopList
            stops={sortedStops}
            onReorder={handleReorder}
            onRemoveStop={handleRemoveStop}
            readOnly={readOnly}
          />

          {!readOnly && activeDay ? (
            <>
              <Button
                variant="outline"
                className="w-full border-dashed"
                onClick={() => setAddStopDialogOpen(true)}
              >
                <Plus /> Add a stop
              </Button>
              <AddStopDialog
                open={addStopDialogOpen}
                onOpenChange={setAddStopDialogOpen}
                tripId={trip.id}
                dayId={activeDay.id}
                token={token}
                onPick={handleAddStop}
              />
            </>
          ) : null}
        </div>

        <ItineraryMap
          stops={sortedStops}
          dayId={activeDay?.id ?? null}
          className="min-h-80 lg:sticky lg:top-6 lg:self-start lg:h-[calc(100svh-8rem)]"
        />
      </div>
    </div>
  )
}
