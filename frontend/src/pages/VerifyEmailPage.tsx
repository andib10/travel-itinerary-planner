import { useEffect, useRef, useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
import { CheckCircle2, XCircle } from "lucide-react"

import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { verifyEmail, ApiError } from "@/api/client"

type Status = "verifying" | "success" | "error"

function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get("token")
  const [status, setStatus] = useState<Status>("verifying")
  const [error, setError] = useState<string | null>(null)
  // verifyEmail() isn't idempotent; guards against StrictMode's double-invoke calling it twice.
  const requestedToken = useRef<string | null>(null)

  useEffect(() => {
    if (!token) {
      setStatus("error")
      setError("This verification link is missing its token.")
      return
    }
    if (requestedToken.current === token) {
      return
    }
    requestedToken.current = token
    verifyEmail(token)
      .then(() => setStatus("success"))
      .catch((err) => {
        setStatus("error")
        setError(err instanceof ApiError ? err.message : "Something went wrong verifying your email.")
      })
  }, [token])

  return (
    <main className="flex min-h-svh flex-col items-center justify-center px-6 py-16">
      <div className="flex w-full max-w-sm flex-col gap-6">
        <Link
          to="/"
          className="mx-auto font-sans text-sm font-medium tracking-tight text-muted-foreground transition-colors hover:text-foreground"
        >
          Voyager
        </Link>
        <Card className="w-full">
          <CardContent className="flex flex-col items-center gap-4 p-6 text-center sm:p-8">
            {status === "verifying" && (
              <p className="text-sm text-muted-foreground">Verifying your email…</p>
            )}
            {status === "success" && (
              <>
                <CheckCircle2 className="size-10 text-emerald-500" />
                <div>
                  <h1 className="font-sans text-lg font-semibold tracking-tight text-foreground">
                    Email verified
                  </h1>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Your email address has been confirmed.
                  </p>
                </div>
                <Button size="lg" nativeButton={false} render={<Link to="/trips">Go to My Trips</Link>} />
              </>
            )}
            {status === "error" && (
              <>
                <XCircle className="size-10 text-destructive" />
                <div>
                  <h1 className="font-sans text-lg font-semibold tracking-tight text-foreground">
                    Verification failed
                  </h1>
                  <p className="mt-1 text-sm text-muted-foreground">{error}</p>
                </div>
                <Button variant="ghost" size="lg" nativeButton={false} render={<Link to="/">Back home</Link>} />
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </main>
  )
}

export default VerifyEmailPage
