import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

const STATUS_CONFIG: Record<string, { label: string; className: string }> = {
  DRAFT: {
    label: "Draft",
    className: "bg-muted text-muted-foreground",
  },
  GENERATED: {
    label: "Generated",
    className: "bg-emerald-500/15 text-emerald-700 dark:text-emerald-400",
  },
  CONFIRMED: {
    label: "Confirmed",
    className: "bg-primary/15 text-primary",
  },
}

// Falls back to a plain badge for an unexpected status value.
const FALLBACK_CONFIG = { label: "Unknown", className: "bg-muted text-muted-foreground" }

export function TripStatusBadge({
  status,
  className,
}: {
  status: string
  className?: string
}) {
  const { label, className: statusClassName } = STATUS_CONFIG[status] ?? FALLBACK_CONFIG

  return <Badge className={cn(statusClassName, className)}>{label}</Badge>
}
