import { SiteNav } from "@/components/site-nav"
import { HeroSection } from "@/components/hero-section"
import { HowItWorks } from "@/components/how-it-works"
import { SiteFooter } from "@/components/site-footer"

function HomePage() {
  return (
    <div className="flex min-h-svh flex-col bg-background">
      <SiteNav />
      <main className="flex-1">
        <HeroSection />
        <HowItWorks />
      </main>
      <SiteFooter />
    </div>
  )
}

export default HomePage
