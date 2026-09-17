import { Link } from "react-router-dom"

import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

export function PlanTripButton({
  className,
  size = "lg",
}: {
  className?: string
  size?: "sm" | "lg" | "default"
}) {
  return (
    <Button
      size={size}
      nativeButton={false}
      className={cn(className)}
      render={<Link to="/trips/new">Plan a new trip</Link>}
    />
  )
}
