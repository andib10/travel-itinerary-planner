import * as React from "react"
import { Link, useNavigate, useSearchParams } from "react-router-dom"

import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { ApiError, resetPassword } from "@/api/client"

function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get("token")
  const navigate = useNavigate()

  const [newPassword, setNewPassword] = React.useState("")
  const [isSubmitting, setIsSubmitting] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  const passwordId = React.useId()

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token) {
      setError("This reset link is missing its token.")
      return
    }
    setError(null)
    setIsSubmitting(true)
    try {
      await resetPassword(token, newPassword)
      // Redirect to login rather than auto-logging in.
      navigate("/login")
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong — please try again.")
    } finally {
      setIsSubmitting(false)
    }
  }

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
          <CardContent className="p-6 sm:p-8">
            <form onSubmit={handleSubmit} className="flex flex-col gap-6" noValidate>
              <h1 className="font-sans text-2xl font-semibold tracking-tight text-balance">
                Set a new password
              </h1>

              <div className="flex flex-col gap-2">
                <Label htmlFor={passwordId}>New password</Label>
                <Input
                  id={passwordId}
                  name="newPassword"
                  type="password"
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  aria-invalid={error ? true : undefined}
                  className="h-10"
                />
              </div>

              {error && (
                <p role="alert" className="text-sm text-destructive">
                  {error}
                </p>
              )}

              <Button type="submit" size="lg" className="w-full" disabled={isSubmitting}>
                Reset password
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </main>
  )
}

export default ResetPasswordPage
