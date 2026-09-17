export interface TripSummaryResponse {
  id: number
  destination: string
  startDate: string
  endDate: string
  budget: string | null
  status: string // "DRAFT" | "GENERATED" | "CONFIRMED"
}

export interface StopResponse {
  id: number
  poiId: number
  poiName: string
  lat: number
  lng: number
  startTime: string // "HH:mm:ss"
  endTime: string
  notes: string | null
  orderIndex: number
}

export interface ItineraryDayResponse {
  id: number
  dayNumber: number
  date: string // "YYYY-MM-DD"
  stops: StopResponse[]
}

export interface TripDetailResponse {
  id: number
  destination: string
  startDate: string
  endDate: string
  budget: string | null
  status: string
  days: ItineraryDayResponse[]
}

export interface GenerateTripRequest {
  destination: string
  startDate: string // "YYYY-MM-DD"
  endDate: string
  interests: string[]
  avoid: string[]
  pace: string // "relaxed" | "packed"
}

export interface OptimizationResult {
  conflictsBefore: number
  conflictsAfter: number
  // Rescheduled in place (excludes drops).
  resolvedCount: number
  // Removed entirely because no valid slot existed.
  droppedCount: number
  unresolvedCount: number
  stopsReordered: boolean
  details: string[]
}

export interface OptimizeDayResponse {
  day: ItineraryDayResponse
  optimization: OptimizationResult
}

// A POI offered by the "add a stop" picker, not yet a scheduled Stop.
export interface SuggestedStopResponse {
  id: number
  name: string
  category: string
  description: string | null
  lat: number
  lng: number
}
