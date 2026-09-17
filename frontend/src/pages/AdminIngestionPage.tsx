import { Link } from "react-router-dom"
import { ArrowLeft } from "lucide-react"

import { IngestCityForm } from "@/components/admin/ingest-city-form"

function AdminIngestionPage() {
  return (
    <div className="min-h-svh bg-background">
      <header className="w-full border-b border-border/60">
        <nav className="mx-auto flex h-16 max-w-3xl items-center justify-between px-6">
          <Link
            to="/trips"
            className="inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
          >
            <ArrowLeft className="size-4" />
            My Trips
          </Link>
          <Link
            to="/admin"
            className="font-sans text-lg font-medium tracking-tight text-foreground"
          >
            Voyager Admin
          </Link>
        </nav>
      </header>

      <main className="mx-auto max-w-xl px-6 py-10">
        <div className="mb-8 flex flex-col gap-2">
          <h1 className="font-sans text-2xl font-semibold tracking-tight text-foreground">
            Ingest city data
          </h1>
          <p className="text-sm text-muted-foreground">
            Fetch points of interest for a city and prepare it for itinerary
            generation.
          </p>
        </div>

        <IngestCityForm />
      </main>
    </div>
  )
}

export default AdminIngestionPage
