import { useEffect, useState } from "react"
import { Loader2, Search, TriangleAlert } from "lucide-react"

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { suggestStops } from "@/api/trips"
import { ApiError } from "@/api/client"
import type { SuggestedStopResponse } from "@/types/trip"

export function AddStopDialog({
  open,
  onOpenChange,
  tripId,
  dayId,
  token,
  /** Adds the chosen POI as a stop. Resolve on success (closes dialog) or reject with an Error to show inline. */
  onPick,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  tripId: number
  dayId: number
  token: string
  onPick: (poi: SuggestedStopResponse) => Promise<void>
}) {
  const [suggestions, setSuggestions] = useState<SuggestedStopResponse[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [query, setQuery] = useState("")
  const [addingId, setAddingId] = useState<number | null>(null)
  const [addError, setAddError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    setSuggestions(null)
    setLoadError(null)
    setAddError(null)
    setQuery("")
    suggestStops(tripId, dayId, token)
      .then(setSuggestions)
      .catch((err) => {
        setLoadError(err instanceof ApiError ? err.message : "Couldn't load suggestions.")
      })
  }, [open, tripId, dayId, token])

  const normalizedQuery = query.trim().toLowerCase()
  const filtered = (suggestions ?? []).filter(
    (poi) =>
      !normalizedQuery ||
      poi.name.toLowerCase().includes(normalizedQuery) ||
      poi.category.toLowerCase().includes(normalizedQuery)
  )

  async function handlePick(poi: SuggestedStopResponse) {
    if (addingId != null) return
    setAddingId(poi.id)
    setAddError(null)
    try {
      await onPick(poi)
      onOpenChange(false)
    } catch (err) {
      setAddError(err instanceof Error ? err.message : "Couldn't add that stop.")
    } finally {
      setAddingId(null)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Add a stop</DialogTitle>
          <DialogDescription>
            Real, verified places for this trip's city that aren't already on your itinerary.
          </DialogDescription>
        </DialogHeader>

        <div className="relative">
          <Search className="pointer-events-none absolute top-1/2 left-2.5 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Search by name or category…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="pl-7"
          />
        </div>

        {addError ? (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive"
          >
            <TriangleAlert className="mt-0.5 size-4 shrink-0" />
            <span>{addError}</span>
          </div>
        ) : null}

        <div className="max-h-80 overflow-y-auto rounded-lg ring-1 ring-foreground/10">
          {loadError ? (
            <p className="p-4 text-sm text-destructive">{loadError}</p>
          ) : suggestions === null ? (
            <p className="p-4 text-sm text-muted-foreground">Loading suggestions…</p>
          ) : filtered.length === 0 ? (
            <p className="p-4 text-sm text-muted-foreground">
              {suggestions.length === 0
                ? "Every known place for this city is already on your itinerary."
                : "No places match your search."}
            </p>
          ) : (
            <ul className="divide-y divide-border">
              {filtered.map((poi) => (
                <li key={poi.id}>
                  <button
                    type="button"
                    disabled={addingId != null}
                    onClick={() => handlePick(poi)}
                    className="flex w-full flex-col gap-1 p-3 text-left transition-colors hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-medium text-foreground">{poi.name}</span>
                      {addingId === poi.id ? (
                        <Loader2 className="size-4 shrink-0 animate-spin text-muted-foreground" />
                      ) : (
                        <Badge variant="outline" className="shrink-0">
                          {poi.category}
                        </Badge>
                      )}
                    </div>
                    {poi.description ? (
                      <p className="line-clamp-2 text-sm text-muted-foreground">{poi.description}</p>
                    ) : null}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
