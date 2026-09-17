import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination"
import { cn } from "@/lib/utils"

// Previous / "Page X of Y" / Next only - no numbered page list needed for these admin tables.
export function TablePagination({
  page,
  totalPages,
  onPageChange,
  className,
}: {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
  className?: string
}) {
  if (totalPages <= 1) return null

  const atStart = page <= 1
  const atEnd = page >= totalPages

  return (
    <Pagination className={cn("justify-between", className)}>
      <PaginationContent>
        <PaginationItem>
          <PaginationPrevious
            href="#"
            aria-disabled={atStart}
            className={cn(atStart && "pointer-events-none opacity-50")}
            onClick={(event) => {
              event.preventDefault()
              if (!atStart) onPageChange(page - 1)
            }}
          />
        </PaginationItem>
      </PaginationContent>

      <span className="text-sm text-muted-foreground">
        Page {page} of {totalPages}
      </span>

      <PaginationContent>
        <PaginationItem>
          <PaginationNext
            href="#"
            aria-disabled={atEnd}
            className={cn(atEnd && "pointer-events-none opacity-50")}
            onClick={(event) => {
              event.preventDefault()
              if (!atEnd) onPageChange(page + 1)
            }}
          />
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  )
}
