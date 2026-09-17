import { Link } from "react-router-dom"
import { ArrowLeft, Database, MapPin, Route, Users } from "lucide-react"

import { Card, CardContent } from "@/components/ui/card"

const ADMIN_LINKS = [
  {
    to: "/admin/ingestion",
    title: "Ingest city data",
    description: "Fetch points of interest for a city and prepare it for itinerary generation.",
    icon: Database,
  },
  {
    to: "/admin/pois",
    title: "Manage points of interest",
    description: "Review, edit, and remove the places available for itinerary generation.",
    icon: MapPin,
  },
  {
    to: "/admin/users",
    title: "Users",
    description: "Review users across the platform.",
    icon: Users,
  },
  {
    to: "/admin/trips",
    title: "Trips",
    description: "Review trips across the platform.",
    icon: Route,
  },
]

// Entry point linking to the admin sub-pages.
function AdminDashboardPage() {
  return (
    <div className="min-h-svh bg-background">
      <header className="w-full border-b border-border/60">
        <nav className="mx-auto flex h-16 max-w-5xl items-center justify-between px-6">
          <Link
            to="/trips"
            className="inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
          >
            <ArrowLeft className="size-4" />
            My Trips
          </Link>
          <span className="font-sans text-lg font-medium tracking-tight text-foreground">
            Voyager Admin
          </span>
        </nav>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-10">
        <div className="mb-8 flex flex-col gap-2">
          <h1 className="font-sans text-2xl font-semibold tracking-tight text-foreground">
            Admin
          </h1>
          <p className="text-sm text-muted-foreground">
            Manage POI data, ingestion, and platform oversight.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {ADMIN_LINKS.map(({ to, title, description, icon: Icon }) => (
            <Link key={to} to={to} className="group">
              <Card className="h-full transition-shadow group-hover:shadow-sm">
                <CardContent className="flex flex-col gap-3">
                  <span className="inline-flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <Icon className="size-4" />
                  </span>
                  <div>
                    <p className="font-medium text-foreground">{title}</p>
                    <p className="mt-1 text-sm text-muted-foreground">{description}</p>
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      </main>
    </div>
  )
}

export default AdminDashboardPage
