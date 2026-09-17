import { useEffect, useState } from "react"

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Label } from "@/components/ui/label"
import { BooleanCell } from "@/components/admin/boolean-cell"
import { PoiEditDialog, type PoiEditValues } from "@/components/admin/poi-edit-dialog"
import { PoiDeleteDialog } from "@/components/admin/poi-delete-dialog"
import { TablePagination } from "@/components/admin/table-pagination"
import { usePagination } from "@/hooks/usePagination"
import { useAuth } from "@/context/AuthContext"
import { getAdminPois, updatePoi, deletePoi } from "@/api/admin"
import { ApiError } from "@/api/client"
import type { AdminPoiResponse } from "@/types/admin"

const ALL_CITIES = "all"

export function PoiTable() {
  const { token } = useAuth()
  const [pois, setPois] = useState<AdminPoiResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cityFilter, setCityFilter] = useState<string>(ALL_CITIES)
  // Distinct cities for the dropdown, captured once from the unfiltered fetch.
  const [cities, setCities] = useState<string[]>([])
  // Distinct categories for the edit dialog's dropdown, same approach.
  const [categories, setCategories] = useState<string[]>([])
  const [editing, setEditing] = useState<AdminPoiResponse | null>(null)
  const [deleting, setDeleting] = useState<AdminPoiResponse | null>(null)

  // Resets to page 1 on a city filter change, but not on an in-place edit/delete.
  const { page, setPage, totalPages, pageItems } = usePagination(pois ?? [], 10, cityFilter)

  useEffect(() => {
    if (!token) return
    setError(null)
    const cityParam = cityFilter === ALL_CITIES ? null : cityFilter
    getAdminPois(cityParam, token)
      .then((data) => {
        setPois(data)
        if (cityParam === null) {
          setCities(Array.from(new Set(data.map((poi) => poi.city))).sort())
          setCategories(Array.from(new Set(data.map((poi) => poi.category))).sort())
        }
      })
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : "Failed to load points of interest")
      })
  }, [cityFilter, token])

  async function handleSave(poiId: number, values: PoiEditValues) {
    if (!token) return
    const updated = await updatePoi(poiId, values, token)
    setPois((current) => current?.map((poi) => (poi.id === poiId ? updated : poi)) ?? null)
  }

  async function handleDelete(poiId: number) {
    if (!token) return
    await deletePoi(poiId, token)
    setPois((current) => current?.filter((poi) => poi.id !== poiId) ?? null)
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-2">
        <Label htmlFor="city-filter" className="text-muted-foreground">
          City
        </Label>
        <Select value={cityFilter} onValueChange={setCityFilter}>
          <SelectTrigger id="city-filter" size="sm" className="w-48">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectGroup>
              <SelectItem value={ALL_CITIES}>All cities</SelectItem>
              {cities.map((city) => (
                <SelectItem key={city} value={city}>
                  {city}
                </SelectItem>
              ))}
            </SelectGroup>
          </SelectContent>
        </Select>
        {pois && (
          <span className="ml-auto text-sm text-muted-foreground">
            {pois.length} {pois.length === 1 ? "place" : "places"}
          </span>
        )}
      </div>

      {error ? (
        <p className="text-sm text-destructive">{error}</p>
      ) : pois === null ? (
        <p className="text-sm text-muted-foreground">Loading points of interest…</p>
      ) : (
        <div className="overflow-hidden rounded-xl ring-1 ring-foreground/10">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/50">
                <TableHead>Name</TableHead>
                <TableHead>Category</TableHead>
                <TableHead className="text-center">Has description</TableHead>
                <TableHead className="text-center">Has embedding</TableHead>
                <TableHead className="w-0 text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {pois.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="h-24 text-center text-muted-foreground">
                    No points of interest for this city yet.
                  </TableCell>
                </TableRow>
              ) : (
                pageItems.map((poi) => (
                  <TableRow key={poi.id}>
                    <TableCell className="font-medium text-foreground">{poi.name}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{poi.category}</Badge>
                    </TableCell>
                    <TableCell className="text-center">
                      <BooleanCell value={poi.hasDescription} label="Has description" />
                    </TableCell>
                    <TableCell className="text-center">
                      <BooleanCell value={poi.hasEmbedding} label="Has embedding" />
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => setEditing(poi)}>
                          Edit
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                          onClick={() => setDeleting(poi)}
                        >
                          Delete
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      )}

      <TablePagination page={page} totalPages={totalPages} onPageChange={setPage} />

      <PoiEditDialog
        poi={editing}
        categories={categories}
        open={editing !== null}
        onOpenChange={(open) => {
          if (!open) setEditing(null)
        }}
        onSave={handleSave}
      />

      <PoiDeleteDialog
        poi={deleting}
        open={deleting !== null}
        onOpenChange={(open) => {
          if (!open) setDeleting(null)
        }}
        onConfirm={handleDelete}
      />
    </div>
  )
}
