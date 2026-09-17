import { apiFetch } from "@/api/client"
import type {
  GenerateTripRequest,
  OptimizeDayResponse,
  StopResponse,
  SuggestedStopResponse,
  TripDetailResponse,
  TripSummaryResponse,
} from "@/types/trip"

export function getMyTrips(token: string) {
  return apiFetch<TripSummaryResponse[]>("/api/trips", { method: "GET" }, token)
}

export function generateTrip(request: GenerateTripRequest, token: string) {
  return apiFetch<TripDetailResponse>(
    "/api/trips/generate",
    { method: "POST", body: JSON.stringify(request) },
    token
  )
}

export function getTripDetail(id: number, token: string) {
  return apiFetch<TripDetailResponse>(`/api/trips/${id}`, { method: "GET" }, token)
}

export function reorderStop(stopId: number, newOrderIndex: number, token: string) {
  return apiFetch<StopResponse>(
    `/api/stops/${stopId}/reorder`,
    { method: "PUT", body: JSON.stringify({ newOrderIndex }) },
    token
  )
}

export function deleteStop(stopId: number, token: string) {
  return apiFetch<void>(`/api/stops/${stopId}`, { method: "DELETE" }, token)
}

// Deletes a trip and its days/stops. 404s if it isn't the caller's.
export function deleteTrip(tripId: number, token: string) {
  return apiFetch<void>(`/api/trips/${tripId}`, { method: "DELETE" }, token)
}

// Candidate POIs for the "add a stop" picker, excluding ones already used in the trip.
export function suggestStops(tripId: number, dayId: number, token: string) {
  return apiFetch<SuggestedStopResponse[]>(
    `/api/trips/${tripId}/days/${dayId}/suggestions`,
    { method: "GET" },
    token
  )
}

// Adds a suggestion as a stop. startTime/endTime are placeholders; the caller
// should immediately call optimizeDay with resetTimeForStopId to compute the real time.
export function addStop(dayId: number, poiId: number, orderIndex: number, token: string) {
  return apiFetch<StopResponse>(
    `/api/days/${dayId}/stops`,
    {
      method: "POST",
      body: JSON.stringify({ poiId, orderIndex, startTime: "09:00:00", endTime: "10:00:00" }),
    },
    token
  )
}

// resetTimeForStopId: recompute this stop's time fresh (e.g. after a drag).
// allowGapCompression: also close an oversized idle gap if present.
export function optimizeDay(
  tripId: number,
  dayId: number,
  token: string,
  resetTimeForStopId?: number,
  allowGapCompression?: boolean
) {
  const params = new URLSearchParams()
  if (resetTimeForStopId != null) params.set("resetTimeForStopId", String(resetTimeForStopId))
  if (allowGapCompression) params.set("allowGapCompression", "true")
  const query = params.toString() ? `?${params.toString()}` : ""
  return apiFetch<OptimizeDayResponse>(
    `/api/trips/${tripId}/days/${dayId}/optimize${query}`,
    { method: "POST" },
    token
  )
}
