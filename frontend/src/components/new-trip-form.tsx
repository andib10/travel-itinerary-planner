import * as React from "react"
import { Loader2 } from "lucide-react"
import { useNavigate } from "react-router-dom"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { PaceSelector, type Pace } from "@/components/pace-selector"
import { TagSelector } from "@/components/tag-selector"
import { useAuth } from "@/context/AuthContext"
import { generateTrip } from "@/api/trips"
import { ApiError } from "@/api/client"

const INTEREST_SUGGESTIONS = [
  "History",
  "Local food",
  "Nature",
  "Museums",
  "Nightlife",
  "Shopping",
]

const AVOID_SUGGESTIONS = ["Crowded places", "Long walks", "Late nights"]

export function NewTripForm() {
  const { token } = useAuth()
  const navigate = useNavigate()

  const [destination, setDestination] = React.useState("")
  const [startDate, setStartDate] = React.useState("")
  const [endDate, setEndDate] = React.useState("")
  const [interests, setInterests] = React.useState<string[]>([])
  const [avoid, setAvoid] = React.useState<string[]>([])
  const [pace, setPace] = React.useState<Pace>("relaxed")

  const [error, setError] = React.useState<string | null>(null)
  const [isGenerating, setIsGenerating] = React.useState(false)

  const destinationId = React.useId()
  const startDateId = React.useId()
  const endDateId = React.useId()
  const errorId = React.useId()

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)

    if (!destination.trim()) {
      setError("Please enter a destination.")
      return
    }
    if (!startDate || !endDate) {
      setError("Please choose your start and end dates.")
      return
    }
    if (endDate < startDate) {
      setError("Your end date can't be before your start date.")
      return
    }
    // Backend requires at least one interest; checked here to avoid a wasted call.
    if (interests.length === 0) {
      setError("Please pick at least one interest.")
      return
    }

    setIsGenerating(true)
    try {
      const trip = await generateTrip(
        { destination: destination.trim(), startDate, endDate, interests, avoid, pace },
        token!
      )
      navigate(`/trips/${trip.id}`)
    } catch (err) {
      // Show the backend's real message for actionable errors (404 unsupported
      // destination, 503 upstream overloaded); generic message otherwise.
      console.error("Failed to generate itinerary:", err)
      if (err instanceof ApiError && (err.status === 404 || err.status === 503)) {
        setError(err.message)
      } else {
        setError("Something went wrong generating your itinerary. Please try again.")
      }
      setIsGenerating(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-8" noValidate>
      {/* Destination */}
      <div className="flex flex-col gap-2">
        <Label htmlFor={destinationId}>Destination</Label>
        <Input
          id={destinationId}
          name="destination"
          placeholder="e.g. Rome"
          value={destination}
          onChange={(e) => setDestination(e.target.value)}
          className="h-10"
          autoComplete="off"
        />
      </div>

      {/* Dates */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="flex flex-col gap-2">
          <Label htmlFor={startDateId}>Start date</Label>
          <Input
            id={startDateId}
            name="startDate"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="h-10"
          />
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor={endDateId}>End date</Label>
          <Input
            id={endDateId}
            name="endDate"
            type="date"
            value={endDate}
            min={startDate || undefined}
            onChange={(e) => setEndDate(e.target.value)}
            className="h-10"
          />
        </div>
      </div>

      {/* Interests */}
      <div className="flex flex-col gap-3">
        <span className="text-sm font-medium leading-none">Interests</span>
        <TagSelector
          ariaLabel="Interests"
          suggestions={INTEREST_SUGGESTIONS}
          value={interests}
          onChange={setInterests}
        />
      </div>

      {/* Avoid */}
      <div className="flex flex-col gap-3">
        <span className="text-sm font-medium leading-none">
          Avoid{" "}
          <span className="font-normal text-muted-foreground">(optional)</span>
        </span>
        <TagSelector
          ariaLabel="Things to avoid"
          suggestions={AVOID_SUGGESTIONS}
          value={avoid}
          onChange={setAvoid}
        />
      </div>

      {/* Pace */}
      <div className="flex flex-col gap-3">
        <span className="text-sm font-medium leading-none">Pace</span>
        <PaceSelector ariaLabel="Trip pace" value={pace} onChange={setPace} />
      </div>

      <Button
        type="submit"
        size="lg"
        className="w-full"
        disabled={isGenerating}
        aria-describedby={error ? errorId : undefined}
      >
        {isGenerating ? (
          <>
            <Loader2 className="size-4 animate-spin" aria-hidden="true" />
            Generating your itinerary...
          </>
        ) : (
          "Generate itinerary"
        )}
      </Button>

      <p
        id={errorId}
        role="alert"
        aria-hidden={error ? undefined : true}
        className={
          error ? "-mt-4 text-sm text-destructive" : "sr-only"
        }
      >
        {error}
      </p>
    </form>
  )
}
