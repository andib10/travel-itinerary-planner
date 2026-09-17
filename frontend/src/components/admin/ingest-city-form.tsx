import { useState } from "react"
import { Loader2 } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useAuth } from "@/context/AuthContext"
import { ingestCity } from "@/api/admin"
import { ApiError } from "@/api/client"

type IngestResult =
  | { status: "success"; saved: number; embedded: number; city: string }
  | { status: "error"; message: string }

export function IngestCityForm() {
  const { token } = useAuth()
  const [city, setCity] = useState("")
  const [isIngesting, setIsIngesting] = useState(false)
  const [result, setResult] = useState<IngestResult | null>(null)

  async function handleIngest() {
    const trimmed = city.trim()
    if (!trimmed || isIngesting || !token) return

    setIsIngesting(true)
    setResult(null)

    try {
      // No client-side timeout - ingestion can take a few minutes.
      const data = await ingestCity(trimmed, token)
      setResult({ status: "success", saved: data.saved, embedded: data.embedded, city: data.city })
    } catch (error) {
      setResult({
        status: "error",
        message: error instanceof ApiError ? error.message : "Ingestion failed. Check the backend logs for details.",
      })
    } finally {
      setIsIngesting(false)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <Label htmlFor="city-name">City name</Label>
        <Input
          id="city-name"
          value={city}
          onChange={(event) => setCity(event.target.value)}
          onKeyDown={(event) => {
            if (
              event.key === "Enter" &&
              !event.nativeEvent.isComposing &&
              event.keyCode !== 229
            ) {
              handleIngest()
            }
          }}
          placeholder="e.g. Barcelona"
          disabled={isIngesting}
          autoComplete="off"
        />
      </div>

      <div>
        <Button
          onClick={handleIngest}
          disabled={isIngesting || city.trim().length === 0}
        >
          {isIngesting ? (
            <>
              <Loader2 className="animate-spin" />
              Ingesting... this may take a few minutes
            </>
          ) : (
            "Ingest city"
          )}
        </Button>
      </div>

      {result ? <IngestResultCard result={result} /> : null}
    </div>
  )
}

function IngestResultCard({ result }: { result: IngestResult }) {
  const isSuccess = result.status === "success"

  return (
    <Card size="sm" role="status" aria-live="polite">
      <CardContent className="flex items-start gap-3">
        <span aria-hidden="true" className={cnDot(isSuccess)} />
        <div className="flex flex-col gap-0.5">
          <p className="text-sm font-medium text-foreground">
            {isSuccess ? "Ingestion complete" : "Ingestion failed"}
          </p>
          <p className="text-sm text-muted-foreground">
            {isSuccess
              ? `Saved ${result.saved} points of interest for ${result.city} (${result.embedded} embedded).`
              : result.message}
          </p>
        </div>
      </CardContent>
    </Card>
  )
}

function cnDot(isSuccess: boolean) {
  return [
    "mt-1 size-2 shrink-0 rounded-full",
    isSuccess ? "bg-primary" : "bg-destructive",
  ].join(" ")
}
