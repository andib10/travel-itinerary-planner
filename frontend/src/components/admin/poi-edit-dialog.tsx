import { useEffect, useState } from "react"
import { Loader2, TriangleAlert } from "lucide-react"

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import type { AdminPoiResponse } from "@/types/admin"

export type PoiEditValues = {
  name: string
  description: string
  category: string
}

type PoiEditDialogProps = {
  /** The POI being edited, or null when the dialog is closed. */
  poi: AdminPoiResponse | null
  /** Real distinct categories already in the DB (PoiTable derives these), not an invented list. */
  categories: string[]
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Called with the edited values when the user saves; rejects to keep the dialog open with an error. */
  onSave: (poiId: number, values: PoiEditValues) => Promise<void>
}

export function PoiEditDialog({ poi, categories, open, onOpenChange, onSave }: PoiEditDialogProps) {
  const [name, setName] = useState("")
  const [description, setDescription] = useState("")
  const [category, setCategory] = useState("")
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Sync local form state whenever a new POI is opened for editing.
  useEffect(() => {
    if (poi) {
      setName(poi.name)
      setDescription(poi.description ?? "")
      setCategory(poi.category)
      setError(null)
    }
  }, [poi])

  // Always include the POI's current category, even if missing from the derived list.
  const categoryOptions = Array.from(
    new Set([...categories, poi?.category].filter((c): c is string => !!c))
  ).sort()

  async function handleSave() {
    if (!poi || isSaving) return
    setIsSaving(true)
    setError(null)
    try {
      await onSave(poi.id, {
        name: name.trim(),
        description: description.trim(),
        category: category.trim(),
      })
      onOpenChange(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to save this point of interest.")
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit point of interest</DialogTitle>
          <DialogDescription>
            Update the details used when this place appears in an itinerary.
          </DialogDescription>
        </DialogHeader>

        <FieldGroup>
          <Field>
            <FieldLabel htmlFor="poi-name">Name</FieldLabel>
            <Input
              id="poi-name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="e.g. Sagrada Família"
              autoComplete="off"
            />
          </Field>

          <Field>
            <FieldLabel htmlFor="poi-description">Description</FieldLabel>
            <Textarea
              id="poi-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Short description shown in itineraries"
              rows={4}
            />
          </Field>

          <Field>
            <FieldLabel htmlFor="poi-category">Category</FieldLabel>
            {/* Real categories from the DB, not a hardcoded enum. */}
            <Select value={category} onValueChange={setCategory}>
              <SelectTrigger id="poi-category" className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  {categoryOptions.map((option) => (
                    <SelectItem key={option} value={option}>
                      {option}
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
          </Field>
        </FieldGroup>

        {error ? (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive"
          >
            <TriangleAlert className="mt-0.5 size-4 shrink-0" />
            <span>{error}</span>
          </div>
        ) : null}

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSaving}>
            Cancel
          </Button>
          <Button onClick={handleSave} disabled={isSaving || name.trim().length === 0}>
            {isSaving ? (
              <>
                <Loader2 className="animate-spin" />
                Saving...
              </>
            ) : (
              "Save"
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
