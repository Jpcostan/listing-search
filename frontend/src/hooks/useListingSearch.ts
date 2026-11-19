import { useEffect, useState } from 'react'
import { searchListings } from '../api/client'
import type { SearchPage, SearchParams } from '../types'

/** Everything the UI needs to know about the current search. */
export interface ListingSearchState {
  data: SearchPage | null
  loading: boolean
  error: string | null
}

/** The outcome of one finished request, remembered together with the params that produced it. */
interface FinishedRequest {
  params: SearchParams
  data: SearchPage | null
  error: string | null
}

/**
 * Fetches results whenever params change.
 * We are "loading" whenever the outcome we hold is not the answer to the current params.
 * A response is ignored if a newer request has started.
 */
export function useListingSearch(params: SearchParams): ListingSearchState {
  const [finished, setFinished] = useState<FinishedRequest | null>(null)

  useEffect(() => {
    // Set to true by the cleanup function when params change again before this request finishes.
    let outdated = false

    searchListings(params)
      .then((page) => {
        if (!outdated) {
          setFinished({ params, data: page, error: null })
        }
      })
      .catch((err: unknown) => {
        if (!outdated) {
          const message = err instanceof Error ? err.message : 'Something went wrong'
          setFinished({ params, data: null, error: message })
        }
      })

    return () => {
      outdated = true
    }
  }, [params])

  if (finished === null || finished.params !== params) {
    return { data: null, loading: true, error: null }
  }
  return { data: finished.data, loading: false, error: finished.error }
}
