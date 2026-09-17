import { Link, useNavigate } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { EmailVerificationBanner } from "@/components/email-verification-banner"
import { useAuth } from "@/context/AuthContext"

export function SiteNav() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate("/")
  }

  return (
    <header className="w-full border-b border-border/60">
      <nav className="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
        <Link
          to="/"
          className="font-sans text-lg font-medium tracking-tight text-foreground"
        >
          Voyager
        </Link>
        <div className="flex items-center gap-2">
          {user ? (
            <>
              <Button
                variant="ghost"
                size="lg"
                nativeButton={false}
                render={<Link to="/trips">My Trips</Link>}
              />
              {user.role === "ADMIN" && (
                <Button
                  variant="ghost"
                  size="lg"
                  nativeButton={false}
                  render={<Link to="/admin">Admin</Link>}
                />
              )}
              <Button
                variant="ghost"
                size="lg"
                nativeButton={false}
                render={<Link to="/account">{user.email}</Link>}
              />
              <Button variant="ghost" size="lg" onClick={handleLogout}>
                Log out
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="ghost"
                size="lg"
                nativeButton={false}
                render={<Link to="/login">Log in</Link>}
              />
              <Button
                size="lg"
                nativeButton={false}
                render={<Link to="/register">Sign up</Link>}
              />
            </>
          )}
        </div>
      </nav>
      {user && !user.emailVerified && <EmailVerificationBanner />}
    </header>
  )
}
