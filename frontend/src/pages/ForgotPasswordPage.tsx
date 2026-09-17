import * as React from "react"
import { Link } from "react-router-dom"

import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { forgotPassword } from "@/api/client"

function ForgotPasswordPage() {
  const [email, setEmail] = React.useState("")
  const [isSubmitting, setIsSubmitting] = React.useState(false)
  // No separate error state - always shows the same generic success message.
  const [status, setStatus] = React.useState<"idle" | "sent" | "error">("idle")

  const emailId = React.useId()

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSubmitting(true)
    try {
      await forgotPassword(email)
      setStatus("sent")
    } catch {
      // ApiError message isn't shown.
      setStatus("error")
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
            {status === "sent" ? (
              <div className="flex flex-col gap-4 text-center">
                <h1 className="font-sans text-2xl font-semibold tracking-tight text-balance">
                  Check your inbox
                </h1>
                <p className="text-sm text-muted-foreground">
                  If an account with that email exists, a password reset link has been sent.
                </p>
                <Button variant="ghost" size="lg" nativeButton={false} render={<Link to="/login">Back to login</Link>} />
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="flex flex-col gap-6" noValidate>
                <div>
                  <h1 className="font-sans text-2xl font-semibold tracking-tight text-balance">
                    Forgot password?
                  </h1>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Enter your email and we'll send you a reset link.
                  </p>
                </div>

                <div className="flex flex-col gap-2">
                  <Label htmlFor={emailId}>Email</Label>
                  <Input
                    id={emailId}
                    name="email"
                    type="email"
                    autoComplete="email"
                    placeholder="you@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="h-10"
                  />
                </div>

                {status === "error" && (
                  <p role="alert" className="text-sm text-destructive">
                    Something went wrong — please try again.
                  </p>
                )}

                <Button type="submit" size="lg" className="w-full" disabled={isSubmitting}>
                  Send reset link
                </Button>

                <p className="text-center text-sm text-muted-foreground">
                  <Link to="/login" className="font-medium text-foreground underline-offset-4 hover:underline">
                    Back to login
                  </Link>
                </p>
              </form>
            )}
          </CardContent>
        </Card>
      </div>
    </main>
  )
}

export default ForgotPasswordPage
