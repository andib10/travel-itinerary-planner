import { Check, X } from "lucide-react"

import { cn } from "@/lib/utils"

type BooleanCellProps = {
  value: boolean
  /** Accessible label describing what the value represents. */
  label: string
}

/** Renders a checkmark for true and an x for false, with an sr-only label. */
export function BooleanCell({ value, label }: BooleanCellProps) {
  return (
    <span
      className={cn(
        "inline-flex size-5 items-center justify-center rounded-full",
        value ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground"
      )}
    >
      {value ? <Check className="size-3.5" /> : <X className="size-3.5" />}
      <span className="sr-only">
        {label}: {value ? "yes" : "no"}
      </span>
    </span>
  )
}
