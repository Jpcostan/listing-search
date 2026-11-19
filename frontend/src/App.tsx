import { useState } from 'react'
import { Pagination } from './components/Pagination'
import { ResultsTable } from './components/ResultsTable'
import { SearchForm } from './components/SearchForm'
import { useListingSearch } from './hooks/useListingSearch'
import { EMPTY_FILTERS } from './types'
import type { SearchParams } from './types'

const PAGE_SIZE = 10

/**
 * The page. Owns the current filters and page number, passes them to useListingSearch,
 * and shows exactly one of: loading, error, no results, or the results table with pagination.
 * Submitting the form resets to page 1.
 */
function App() {
  const [params, setParams] = useState<SearchParams>({ ...EMPTY_FILTERS, page: 1, pageSize: PAGE_SIZE })
  const { data, loading, error } = useListingSearch(params)

  /** Picks the one thing to show under the form. */
  function renderResults() {
    if (loading) {
      return <p>Loading...</p>
    }
    if (error) {
      return <p role="alert">Error: {error}</p>
    }
    if (!data || data.items.length === 0) {
      return <p>No listings match your search.</p>
    }
    return (
      <>
        <ResultsTable items={data.items} />
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          totalItems={data.totalItems}
          onPageChange={(page) => setParams({ ...params, page })}
        />
      </>
    )
  }

  return (
    <main>
      <h1>Listing Search</h1>
      <SearchForm onSearch={(filters) => setParams({ ...filters, page: 1, pageSize: PAGE_SIZE })} />
      {renderResults()}
    </main>
  )
}

export default App
