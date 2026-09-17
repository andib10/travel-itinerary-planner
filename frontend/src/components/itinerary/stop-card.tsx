import { GripVertical, X } from "lucide-react"

import { cn } from "@/lib/utils"
import { formatTimeRange } from "@/lib/trips"
import type { StopResponse } from "@/types/trip"

export function StopCard({
  stop,
  onRemove,
  dragHandleProps,
  readOnly = false,
  className,
}: {
  stop: StopResponse
  /** Called when the remove ("x") control is pressed. */
  onRemove?: (stopId: number) => void
  /** Spread onto the drag handle (dnd-kit listeners/attributes). */
  dragHandleProps?: React.HTMLAttributes<HTMLButtonElement>
  /** Hides the drag handle and remove control - admin oversight view, not editable there. */
  readOnly?: boolean
  className?: string
}) {
  return (
    <div
      className={cn(
        "flex items-start gap-3 rounded-xl bg-card p-3 text-card-foreground ring-1 ring-foreground/10 transition-shadow hover:shadow-sm",
        className
      )}
    >
      {readOnly ? (
        <span className="mt-0.5 p-1 text-muted-foreground/40" aria-hidden="true">
          <GripVertical className="size-4" />
        </span>
      ) : (
        <button
          type="button"
          aria-label={`Reorder ${stop.poiName}`}
          className="mt-0.5 cursor-grab touch-none rounded-md p-1 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none active:cursor-grabbing"
          {...dragHandleProps}
        >
          <GripVertical className="size-4" />
        </button>
      )}

      <div className="min-w-0 flex-1">
        <p className="font-semibold text-foreground text-pretty">{stop.poiName}</p>
        <p className="mt-0.5 text-sm text-muted-foreground tabular-nums">
          {formatTimeRange(stop.startTime, stop.endTime)}
        </p>
        {stop.notes ? (
          <p className="mt-1 text-sm text-muted-foreground text-pretty">
            {stop.notes}
          </p>
        ) : null}
      </div>

      {readOnly ? null : (
        <button
          type="button"
          aria-label={`Remove ${stop.poiName}`}
          onClick={() => onRemove?.(stop.id)}
          className="rounded-md p-1 text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
        >
          <X className="size-4" />
        </button>
      )}
    </div>
  )
}
