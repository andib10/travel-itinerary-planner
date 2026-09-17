import { cn } from "@/lib/utils"

export type Pace = "relaxed" | "packed"

const OPTIONS: { value: Pace; label: string }[] = [
  { value: "relaxed", label: "Relaxed" },
  { value: "packed", label: "Packed" },
]

interface PaceSelectorProps {
  ariaLabel: string
  value: Pace
  onChange: (next: Pace) => void
}

/**
 * A two-option segmented control implemented as an accessible radio group.
 * Controlled: pass `value` / `onChange` to wire to real state.
 */
export function PaceSelector({ ariaLabel, value, onChange }: PaceSelectorProps) {
  return (
    <div
      role="radiogroup"
      aria-label={ariaLabel}
      className="inline-flex w-full rounded-lg border border-border bg-muted p-1"
    >
      {OPTIONS.map((option) => {
        const selected = value === option.value
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={selected}
            onClick={() => onChange(option.value)}
            className={cn(
              "flex-1 rounded-md px-4 py-2 text-sm font-medium transition-colors",
              "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
              selected
                ? "bg-background text-foreground shadow-sm"
                : "text-muted-foreground hover:text-foreground",
            )}
          >
            {option.label}
          </button>
        )
      })}
    </div>
  )
}
