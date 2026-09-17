import { closestCenter, DndContext, PointerSensor, useSensor, useSensors, type DragEndEvent } from "@dnd-kit/core"
import { SortableContext, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable"
import { CSS } from "@dnd-kit/utilities"

import { StopCard } from "@/components/itinerary/stop-card"
import { cn } from "@/lib/utils"
import type { StopResponse } from "@/types/trip"

function SortableStopItem({
  stop,
  onRemoveStop,
}: {
  stop: StopResponse
  onRemoveStop?: (stopId: number) => void
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: stop.id,
  })

  return (
    <li
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={cn(isDragging && "opacity-50")}
    >
      <StopCard stop={stop} onRemove={onRemoveStop} dragHandleProps={{ ...attributes, ...listeners }} />
    </li>
  )
}

export function StopList({
  stops,
  onRemoveStop,
  onReorder,
  readOnly = false,
  className,
}: {
  /** Expected already sorted by orderIndex. */
  stops: StopResponse[]
  onRemoveStop?: (stopId: number) => void
  /** Called with the moved stop's id and its new 0-based position after a drag. */
  onReorder?: (stopId: number, newIndex: number) => void
  /** Admin oversight view - no drag-and-drop, no remove control, plain list. */
  readOnly?: boolean
  className?: string
}) {
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 4 } }))

  if (stops.length === 0) {
    return (
      <div
        className={cn(
          "flex min-h-40 items-center justify-center rounded-xl border border-dashed border-border p-6 text-center text-sm text-muted-foreground",
          className
        )}
      >
        No stops planned for this day yet.
      </div>
    )
  }

  if (readOnly) {
    return (
      <ol className={cn("flex flex-col gap-3", className)}>
        {stops.map((stop) => (
          <li key={stop.id}>
            <StopCard stop={stop} readOnly />
          </li>
        ))}
      </ol>
    )
  }

  function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event
    if (!over || active.id === over.id) return
    const newIndex = stops.findIndex((stop) => stop.id === over.id)
    if (newIndex === -1) return
    onReorder?.(active.id as number, newIndex)
  }

  return (
    <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
      <SortableContext items={stops.map((stop) => stop.id)} strategy={verticalListSortingStrategy}>
        <ol className={cn("flex flex-col gap-3", className)}>
          {stops.map((stop) => (
            <SortableStopItem key={stop.id} stop={stop} onRemoveStop={onRemoveStop} />
          ))}
        </ol>
      </SortableContext>
    </DndContext>
  )
}
