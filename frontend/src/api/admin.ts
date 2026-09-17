import { apiFetch } from "@/api/client"
import type {
  AdminPoiResponse,
  AdminTripResponse,
  AdminUserResponse,
  IngestCityResponse,
  UpdatePoiRequest,
  UpdateUserRoleRequest,
} from "@/types/admin"
import type { TripDetailResponse } from "@/types/trip"

export function ingestCity(city: string, token: string) {
  return apiFetch<IngestCityResponse>(
    `/api/admin/ingest/${encodeURIComponent(city)}`,
    { method: "POST" },
    token
  )
}

export function getAdminPois(city: string | null, token: string) {
  const query = city ? `?city=${encodeURIComponent(city)}` : ""
  return apiFetch<AdminPoiResponse[]>(`/api/admin/pois${query}`, { method: "GET" }, token)
}

export function updatePoi(id: number, data: UpdatePoiRequest, token: string) {
  return apiFetch<AdminPoiResponse>(
    `/api/admin/pois/${id}`,
    { method: "PUT", body: JSON.stringify(data) },
    token
  )
}

export function deletePoi(id: number, token: string) {
  return apiFetch<void>(`/api/admin/pois/${id}`, { method: "DELETE" }, token)
}

export function getAdminUsers(token: string) {
  return apiFetch<AdminUserResponse[]>("/api/admin/users", { method: "GET" }, token)
}

export function updateUserRole(id: number, role: string, token: string) {
  const body: UpdateUserRoleRequest = { role }
  return apiFetch<AdminUserResponse>(
    `/api/admin/users/${id}/role`,
    { method: "PUT", body: JSON.stringify(body) },
    token
  )
}

export function deleteUser(id: number, token: string) {
  return apiFetch<void>(`/api/admin/users/${id}`, { method: "DELETE" }, token)
}

export function getAdminTrips(token: string) {
  return apiFetch<AdminTripResponse[]>("/api/admin/trips", { method: "GET" }, token)
}

export function getAdminTripDetail(id: number, token: string) {
  return apiFetch<TripDetailResponse>(`/api/admin/trips/${id}`, { method: "GET" }, token)
}

export function deleteAdminTrip(id: number, token: string) {
  return apiFetch<void>(`/api/admin/trips/${id}`, { method: "DELETE" }, token)
}
