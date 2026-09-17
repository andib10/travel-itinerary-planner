export interface AdminPoiResponse {
  id: number
  xid: string
  name: string
  category: string
  city: string
  lat: number
  lng: number
  // null means "unchanged"; empty string means "clear" (see UpdatePoiRequest).
  description: string | null
  hasDescription: boolean
  hasEmbedding: boolean
  hasOpeningHours: boolean
}

export interface UpdatePoiRequest {
  name: string
  description: string
  category: string
}

export interface AdminUserResponse {
  id: number
  email: string
  role: string
  createdAt: string
  tripCount: number
}

// PUT /api/admin/users/{id}/role body. role must be "USER" or "ADMIN".
export interface UpdateUserRoleRequest {
  role: string
}

export interface AdminTripResponse {
  id: number
  ownerEmail: string
  destination: string
  startDate: string
  endDate: string
  status: string
  dayCount: number
}

export interface IngestCityResponse {
  city: string
  saved: number
  embedded: number
}

// AdminTripDetail reuses TripDetailResponse from types/trip.ts.
