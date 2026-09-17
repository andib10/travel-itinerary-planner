import { useEffect, useMemo, useState } from "react"

/**
 * Client-side pagination over a fully-loaded array.
 * @param resetKey Changing this (e.g. a filter) resets back to page 1.
 */
export function usePagination<T>(items: T[], pageSize = 10, resetKey?: unknown) {
  const [page, setPage] = useState(1)

  useEffect(() => {
    setPage(1)
  }, [resetKey])

  const totalPages = Math.max(1, Math.ceil(items.length / pageSize))

  // Clamp back into range if the list shrinks past the current page.
  useEffect(() => {
    setPage((current) => Math.min(current, totalPages))
  }, [totalPages])

  const pageItems = useMemo(
    () => items.slice((page - 1) * pageSize, page * pageSize),
    [items, page, pageSize]
  )

  return { page, setPage, totalPages, pageItems }
}
