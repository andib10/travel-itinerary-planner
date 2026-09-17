import * as React from "react"
import { Link, useNavigate } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useAuth } from "@/context/AuthContext"
import { ApiError } from "@/api/client"

type AuthMode = "login" | "register"

interface AuthFormProps {
  mode: AuthMode
}

const COPY: Record<
  AuthMode,
  {
    heading: string
    submitLabel: string
    footerText: string
    footerLinkLabel: string
    footerLinkHref: string
    fallbackError: string
  }
> = {
  login: {
    heading: "Log in",
    submitLabel: "Log in",
    footerText: "Don't have an account?",
    footerLinkLabel: "Sign up",
    footerLinkHref: "/register",
    fallbackError: "Invalid email or password",
  },
  register: {
    heading: "Create your account",
    submitLabel: "Sign up",
    footerText: "Already have an account?",
    footerLinkLabel: "Log in",
    footerLinkHref: "/login",
    fallbackError: "Something went wrong - please try again",
  },
}

export function AuthForm({ mode }: AuthFormProps) {
  const copy = COPY[mode]
  const { login, register } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = React.useState("")
  const [password, setPassword] = React.useState("")
  const [error, setError] = React.useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = React.useState(false)

  const emailId = React.useId()
  const passwordId = React.useId()
  const errorId = React.useId()

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      if (mode === "login") {
        await login(email, password)
      } else {
        await register(email, password)
      }
      navigate("/trips")
    } catch (err) {
      setError(err instanceof ApiError ? err.message : copy.fallbackError)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6" noValidate>
      <h1 className="font-sans text-2xl font-semibold tracking-tight text-balance">
        {copy.heading}
      </h1>

      <div className="flex flex-col gap-4">
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
            aria-invalid={error ? true : undefined}
            aria-describedby={error ? errorId : undefined}
            className="h-10"
          />
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <Label htmlFor={passwordId}>Password</Label>
            {mode === "login" && (
              <Link
                to="/forgot-password"
                className="text-sm font-medium text-muted-foreground underline-offset-4 hover:underline"
              >
                Forgot password?
              </Link>
            )}
          </div>
          <Input
            id={passwordId}
            name="password"
            type="password"
            autoComplete={mode === "login" ? "current-password" : "new-password"}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            aria-invalid={error ? true : undefined}
            aria-describedby={error ? errorId : undefined}
            className="h-10"
          />
        </div>
      </div>

      <Button type="submit" size="lg" className="w-full" disabled={isSubmitting}>
        {copy.submitLabel}
      </Button>

      <p
        id={errorId}
        role="alert"
        aria-hidden={error ? undefined : true}
        className={error ? "text-sm text-destructive" : "sr-only"}
      >
        {error}
      </p>

      <p className="text-center text-sm text-muted-foreground">
        {copy.footerText}{" "}
        <Link
          to={copy.footerLinkHref}
          className="font-medium text-foreground underline-offset-4 hover:underline"
        >
          {copy.footerLinkLabel}
        </Link>
      </p>
    </form>
  )
}
