import { afterEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse, scoredListing, searchPage } from '../testData'
import type { SearchParams } from '../types'
import { ApiError, searchListings } from './client'

const NO_FILTERS: SearchParams = {
  minPrice: '',
  maxPrice: '',
  minBedrooms: '',
  city: '',
  keyword: '',
  status: '',
  targetBudget: '',
  page: 1,
  pageSize: 10,
}

/** Replaces the browser's fetch with a fake for one test. */
function mockFetch() {
  const fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('searchListings', () => {
  it('leaves empty filters out of the query string', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValue(jsonResponse(searchPage([])))

    await searchListings({ ...NO_FILTERS, city: 'Reston', maxPrice: '600000', keyword: '   ' })

    expect(fetchMock).toHaveBeenCalledWith('/api/listings/search?maxPrice=600000&city=Reston&page=1&pageSize=10')
  })

  it('returns the parsed page on success', async () => {
    const page = searchPage([scoredListing('A1', '123 Main St')])
    mockFetch().mockResolvedValue(jsonResponse(page))

    await expect(searchListings(NO_FILTERS)).resolves.toEqual(page)
  })

  it('throws ApiError with the server message on a 400', async () => {
    mockFetch().mockResolvedValue(jsonResponse({ error: 'minPrice must not be greater than maxPrice' }, 400))

    const request = searchListings(NO_FILTERS)

    await expect(request).rejects.toBeInstanceOf(ApiError)
    await expect(request).rejects.toThrow('minPrice must not be greater than maxPrice')
  })

  it('throws ApiError with the status code when the error body is not JSON', async () => {
    mockFetch().mockResolvedValue(new Response('<html>Bad Gateway</html>', { status: 502 }))

    await expect(searchListings(NO_FILTERS)).rejects.toThrow('The server returned an error (HTTP 502). Is the backend running?')
  })

  it('throws a generic ApiError when the network fails', async () => {
    mockFetch().mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(searchListings(NO_FILTERS)).rejects.toThrow('Could not reach the server. Is the backend running?')
  })
})
