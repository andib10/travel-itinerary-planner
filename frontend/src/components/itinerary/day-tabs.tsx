import { cn } from "@/lib/utils"
import type { ItineraryDayResponse } from "@/types/trip"

export function DayTabs({
  days,
  activeDayId,
  onSelectDay,
  className,
}: {
  days: ItineraryDayResponse[]
  activeDayId: number | null
  onSelectDay: (dayId: number) => void
  className?: string
}) {
  return (
    <div
      role="tablist"
      aria-label="Itinerary days"
      className={cn(
        "flex flex-wrap items-center gap-1 rounded-lg bg-muted p-1",
        className
      )}
    >
      {days.map((day) => {
        const isActive = day.id === activeDayId
        return (
          <button
            key={day.id}
            type="button"
            role="tab"
            aria-selected={isActive}
            onClick={() => onSelectDay(day.id)}
            className={cn(
              "rounded-md px-3 py-1.5 text-sm font-medium transition-colors outline-none focus-visible:ring-2 focus-visible:ring-ring",
              isActive
                ? "bg-background text-foreground shadow-sm"
                : "text-muted-foreground hover:text-foreground"
            )}
          >
            Day {day.dayNumber}
          </button>
        )
      })}
    </div>
  )
}
