import { AlertTriangle } from "lucide-react"

import { cn } from "@/lib/utils"

export function FixScheduleBanner({
  onFix,
  onDismiss,
  message = "Times may need updating",
  fixLabel = "Fix schedule",
  fixingLabel = "Fixing…",
  fixing = false,
  className,
}: {
  /** Called when the action button is pressed. Wire to your API. */
  onFix?: () => void
  /** Called when the banner is dismissed. */
  onDismiss?: () => void
  message?: string
  /** Button text when idle - customize per use (e.g. "Close gap"). */
  fixLabel?: string
  /** Button text while an action is in flight. */
  fixingLabel?: string
  /** True while a request is in flight - disables the button. */
  fixing?: boolean
  className?: string
}) {
  return (
    <div
      role="status"
      className={cn(
        "flex items-center justify-between gap-3 rounded-lg border border-amber-500/30 bg-amber-500/10 px-4 py-2.5 text-sm text-amber-900 dark:text-amber-200",
        className
      )}
    >
      <div className="flex items-center gap-2">
        <AlertTriangle className="size-4 shrink-0 text-amber-600 dark:text-amber-400" />
        <span className="font-medium">{message}</span>
      </div>

      <div className="flex items-center gap-1">
        {onFix ? (
          <FixScheduleButton onClick={onFix} disabled={fixing} label={fixing ? fixingLabel : fixLabel} />
        ) : null}
        {onDismiss ? (
          <button
            type="button"
            aria-label="Dismiss"
            onClick={onDismiss}
            className="rounded-md px-2 py-1 text-xs font-medium text-amber-800 transition-colors hover:bg-amber-500/20 focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none dark:text-amber-200"
          >
            Dismiss
          </button>
        ) : null}
      </div>
    </div>
  )
}

/** The "Fix schedule" action button, split out so it can be wired independently. */
export function FixScheduleButton({
  onClick,
  disabled = false,
  label = "Fix schedule",
  className,
}: {
  onClick?: () => void
  disabled?: boolean
  label?: string
  className?: string
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "rounded-md bg-amber-500 px-2.5 py-1 text-xs font-semibold text-amber-950 transition-colors hover:bg-amber-400 focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-60",
        className
      )}
    >
      {label}
    </button>
  )
}
