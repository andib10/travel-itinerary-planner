import { Link } from "react-router-dom"

import { Card, CardContent } from "@/components/ui/card"
import { NewTripForm } from "@/components/new-trip-form"

function TripRequestPage() {
  return (
    <main className="flex min-h-svh flex-col bg-background">
      <header className="border-b border-border">
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center px-4 sm:px-6">
          <Link
            to="/trips"
            className="font-sans text-lg font-medium tracking-tight"
          >
            Voyager
          </Link>
        </div>
      </header>

      <div className="mx-auto flex w-full max-w-xl flex-1 flex-col justify-center px-4 py-12 sm:px-6">
        <div className="mb-8 flex flex-col gap-2">
          <h1 className="font-sans text-3xl font-semibold tracking-tight text-balance">
            Plan a new trip
          </h1>
          <p className="text-muted-foreground text-pretty">
            A few quick details and we&apos;ll put together your day-by-day plan.
          </p>
        </div>

        <Card>
          <CardContent className="p-6 sm:p-8">
            <NewTripForm />
          </CardContent>
        </Card>
      </div>
    </main>
  )
}

export default TripRequestPage
