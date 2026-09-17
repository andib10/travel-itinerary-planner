import * as React from "react"
import { Check } from "lucide-react"

import { cn } from "@/lib/utils"

interface TagSelectorProps {
  /** Accessible label for the group (visually rendered by the parent). */
  ariaLabel: string
  /** Suggested tags shown as toggle chips. */
  suggestions: string[]
  /** Currently selected tags. */
  value: string[]
  /** Called with the next selected-tags array whenever a chip is toggled. */
  onChange: (next: string[]) => void
}

/**
 * A lightweight multi-select rendered as toggle chips.
 * Addressable + controlled: pass `value` / `onChange` to wire to real state.
 */
export function TagSelector({
  ariaLabel,
  suggestions,
  value,
  onChange,
}: TagSelectorProps) {
  function toggle(tag: string) {
    if (value.includes(tag)) {
      onChange(value.filter((t) => t !== tag))
    } else {
      onChange([...value, tag])
    }
  }

  return (
    <div
      role="group"
      aria-label={ariaLabel}
      className="flex flex-wrap gap-2"
    >
      {suggestions.map((tag) => {
        const selected = value.includes(tag)
        return (
          <button
            key={tag}
            type="button"
            aria-pressed={selected}
            onClick={() => toggle(tag)}
            className={cn(
              "inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-sm font-medium transition-colors",
              "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
              selected
                ? "border-primary bg-primary text-primary-foreground"
                : "border-border bg-transparent text-foreground hover:bg-accent hover:text-accent-foreground",
            )}
          >
            {selected ? (
              <Check className="size-3.5" aria-hidden="true" />
            ) : null}
            {tag}
          </button>
        )
      })}
    </div>
  )
}
