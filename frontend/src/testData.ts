import type { ScoredListing, SearchPage } from './types'

/** Builds one result for tests. Only the fields a test cares about are parameters. */
export function scoredListing(id: string, address: string, possibleDuplicates: string[] = []): ScoredListing {
  return {
    listing: {
      id,
      source: 'MLS_A',
      address,
      city: 'Fairfax',
      state: 'VA',
      zip: '22030',
      price: 450000,
      bedrooms: 2,
      bathrooms: 1.5,
      sqft: 980,
      latitude: 38.8,
      longitude: -77.3,
      listedDate: '2026-11-01',
      status: 'active',
      description: 'Test listing',
    },
    score: 0.875,
    possibleDuplicates,
  }
}

/** Builds a page of results for tests. */
export function searchPage(items: ScoredListing[], page = 1, totalPages = 1): SearchPage {
  return { items, page, pageSize: 10, totalItems: items.length, totalPages }
}

/** A fake fetch Response carrying the given JSON body. */
export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}
