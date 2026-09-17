import { Link } from "react-router-dom"
import { Card, CardContent } from "@/components/ui/card"
import { AuthForm } from "@/components/auth-form"

function LoginPage() {
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
            <AuthForm mode="login" />
          </CardContent>
        </Card>
      </div>
    </main>
  )
}

export default LoginPage
