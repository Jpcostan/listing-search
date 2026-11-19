interface PaginationProps {
  page: number
  totalPages: number
  totalItems: number
  /** Called with the page number the user wants to go to. */
  onPageChange: (page: number) => void
}

/** "Page X of Y (N results)" with Previous and Next buttons, disabled at the ends. */
export function Pagination({ page, totalPages, totalItems, onPageChange }: PaginationProps) {
  return (
    <p>
      <button disabled={page <= 1} onClick={() => onPageChange(page - 1)}>
        Previous
      </button>{' '}
      Page {page} of {totalPages} ({totalItems} results){' '}
      <button disabled={page >= totalPages} onClick={() => onPageChange(page + 1)}>
        Next
      </button>
    </p>
  )
}
