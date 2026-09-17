import * as React from "react"
import { useNavigate } from "react-router-dom"
import { Loader2 } from "lucide-react"

import { SiteNav } from "@/components/site-nav"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useAuth } from "@/context/AuthContext"
import { changePassword, ApiError } from "@/api/client"

function AccountPage() {
  const { user, token, logout } = useAuth()
  const navigate = useNavigate()

  const [currentPassword, setCurrentPassword] = React.useState("")
  const [newPassword, setNewPassword] = React.useState("")
  const [confirmPassword, setConfirmPassword] = React.useState("")
  const [error, setError] = React.useState<string | null>(null)
  const [success, setSuccess] = React.useState(false)
  const [isSubmitting, setIsSubmitting] = React.useState(false)

  const currentPasswordId = React.useId()
  const newPasswordId = React.useId()
  const confirmPasswordId = React.useId()
  const errorId = React.useId()

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)

    if (newPassword.length < 8) {
      setError("New password must be at least 8 characters.")
      return
    }
    if (newPassword !== confirmPassword) {
      setError("New passwords don't match.")
      return
    }

    setIsSubmitting(true)
    try {
      await changePassword(currentPassword, newPassword, token!)
      // Old JWT stays valid, so log out and force a fresh login.
      setSuccess(true)
      setTimeout(() => {
        logout()
        navigate("/login")
      }, 1500)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.")
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-svh bg-background">
      <SiteNav />

      <main className="mx-auto flex max-w-sm flex-col gap-6 px-6 py-16">
        <div className="flex flex-col gap-2">
          <h1 className="font-sans text-2xl font-semibold tracking-tight text-foreground">
            Account
          </h1>
          <p className="text-sm text-muted-foreground">{user?.email}</p>
        </div>

        <Card className="w-full">
          <CardContent className="p-6 sm:p-8">
            {success ? (
              <p className="text-center text-sm text-foreground" role="status">
                Password changed. Redirecting you to log in again&hellip;
              </p>
            ) : (
              <form onSubmit={handleSubmit} className="flex flex-col gap-6" noValidate>
                <h2 className="font-sans text-lg font-semibold tracking-tight text-foreground">
                  Change password
                </h2>

                <div className="flex flex-col gap-4">
                  <div className="flex flex-col gap-2">
                    <Label htmlFor={currentPasswordId}>Current password</Label>
                    <Input
                      id={currentPasswordId}
                      name="currentPassword"
                      type="password"
                      autoComplete="current-password"
                      value={currentPassword}
                      onChange={(e) => setCurrentPassword(e.target.value)}
                      aria-invalid={error ? true : undefined}
                      aria-describedby={error ? errorId : undefined}
                      className="h-10"
                    />
                  </div>

                  <div className="flex flex-col gap-2">
                    <Label htmlFor={newPasswordId}>New password</Label>
                    <Input
                      id={newPasswordId}
                      name="newPassword"
                      type="password"
                      autoComplete="new-password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      aria-invalid={error ? true : undefined}
                      aria-describedby={error ? errorId : undefined}
                      className="h-10"
                    />
                  </div>

                  <div className="flex flex-col gap-2">
                    <Label htmlFor={confirmPasswordId}>Confirm new password</Label>
                    <Input
                      id={confirmPasswordId}
                      name="confirmPassword"
                      type="password"
                      autoComplete="new-password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      aria-invalid={error ? true : undefined}
                      aria-describedby={error ? errorId : undefined}
                      className="h-10"
                    />
                  </div>
                </div>

                <Button type="submit" size="lg" className="w-full" disabled={isSubmitting}>
                  {isSubmitting ? (
                    <>
                      <Loader2 className="size-4 animate-spin" aria-hidden="true" />
                      Changing password&hellip;
                    </>
                  ) : (
                    "Change password"
                  )}
                </Button>

                <p
                  id={errorId}
                  role="alert"
                  aria-hidden={error ? undefined : true}
                  className={error ? "text-sm text-destructive" : "sr-only"}
                >
                  {error}
                </p>
              </form>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  )
}

export default AccountPage
