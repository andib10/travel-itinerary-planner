import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"

export function HeroSection() {
  return (
    <section className="mx-auto max-w-3xl px-6 py-24 text-center sm:py-32">
      <h1 className="font-sans text-4xl font-semibold tracking-tight text-balance text-foreground sm:text-5xl md:text-6xl">
        Plan your next trip in minutes
      </h1>
      <p className="mx-auto mt-6 max-w-xl text-lg text-pretty text-muted-foreground leading-relaxed">
        Tell us where you&apos;re going and what you like — get a ready-to-use,
        day-by-day itinerary you can edit and take with you.
      </p>
      <div className="mt-10 flex flex-col items-center gap-4">
        <Button
          size="lg"
          nativeButton={false}
          className="h-11 px-8 text-base"
          render={<Link to="/register">Get started</Link>}
        />
        <p className="text-sm text-muted-foreground">
          Already have an account?{" "}
          <Link
            to="/login"
            className="font-medium text-primary underline-offset-4 hover:underline"
          >
            Log in
          </Link>
        </p>
      </div>

      <div className="mt-16 overflow-hidden rounded-2xl border border-border/60 shadow-sm">
        <img
          src="/hero-destination.png"
          alt="Sunlit coastal road winding along a turquoise sea toward a seaside village"
          width={1200}
          height={675}
          className="h-auto w-full object-cover"
        />
      </div>
    </section>
  )
}
