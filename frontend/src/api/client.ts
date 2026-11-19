import type { SearchPage, SearchParams } from '../types'

/** An error whose message is safe to show to the user. */
export class ApiError extends Error {}

/**
 * Calls GET /api/listings/search.
 * Leaves empty filters out of the query string.
 * Throws ApiError with the server's message on a non-2xx response, or a generic one if the network fails.
 */
export async function searchListings(params: SearchParams): Promise<SearchPage> {
  const query = new URLSearchParams()
  for (const [name, value] of Object.entries(params)) {
    const text = String(value).trim()
    if (text !== '') {
      query.set(name, text)
    }
  }

  let response: Response
  try {
    response = await fetch(`/api/listings/search?${query}`)
  } catch {
    throw new ApiError('Could not reach the server. Is the backend running?')
  }

  if (!response.ok) {
    // The backend sends {"error": "..."}; fall back to the status code if the body is not JSON.
    const body = await response.json().catch(() => null)
    throw new ApiError(body?.error ?? `The server returned an error (HTTP ${response.status}). Is the backend running?`)
  }

  return response.json()
}
