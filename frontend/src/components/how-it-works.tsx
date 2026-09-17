const steps = [
  {
    number: "1",
    title: "Tell us about your trip",
    description: "Destination, dates, and what you’re interested in.",
  },
  {
    number: "2",
    title: "Get a personalized itinerary",
    description: "A day-by-day plan built around your interests.",
  },
  {
    number: "3",
    title: "Edit and go",
    description: "Reorder stops, fix the schedule, and you’re ready.",
  },
]

export function HowItWorks() {
  return (
    <section className="border-t border-border/60 bg-muted/30">
      <div className="mx-auto max-w-6xl px-6 py-20 sm:py-24">
        <h2 className="text-center font-sans text-2xl font-semibold tracking-tight text-foreground sm:text-3xl">
          How it works
        </h2>
        <div className="mt-14 grid gap-10 sm:grid-cols-3 sm:gap-8">
          {steps.map((step) => (
            <div key={step.number} className="flex flex-col items-start">
              <span className="flex size-10 items-center justify-center rounded-full bg-primary/10 font-sans text-base font-semibold text-primary">
                {step.number}
              </span>
              <h3 className="mt-5 text-lg font-medium text-foreground">
                {step.title}
              </h3>
              <p className="mt-2 text-pretty text-muted-foreground leading-relaxed">
                {step.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
