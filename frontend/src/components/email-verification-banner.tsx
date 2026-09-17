import { Mail } from "lucide-react"

/** Standing reminder shown under SiteNav until the signed-in user verifies their email. */
export function EmailVerificationBanner() {
  return (
    <div
      role="status"
      className="flex items-center justify-center gap-2 border-b border-amber-500/30 bg-amber-500/10 px-4 py-2 text-sm text-amber-900 dark:text-amber-200"
    >
      <Mail className="size-4 shrink-0 text-amber-600 dark:text-amber-400" />
      <span>Please verify your email — check your inbox for the verification link.</span>
    </div>
  )
}
