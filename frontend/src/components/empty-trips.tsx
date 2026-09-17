import { PlanTripButton } from "@/components/plan-trip-button"

export function EmptyTrips() {
  return (
    <div className="flex flex-col items-center justify-center gap-6 rounded-xl border border-dashed border-border py-20 text-center">
      <p className="text-base text-muted-foreground text-pretty">
        You haven&apos;t planned a trip yet
      </p>
      <PlanTripButton />
    </div>
  )
}
