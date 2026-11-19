// Shapes shared by the whole frontend. These mirror the backend's JSON exactly.

export interface Listing {
  id: string
  source: string
  address: string
  city: string
  state: string
  zip: string
  price: number
  bedrooms: number
  bathrooms: number
  sqft: number
  latitude: number
  longitude: number
  listedDate: string
  status: string
  description: string
}

export interface ScoredListing {
  listing: Listing
  score: number
  /** Other listings that look like the same property from another feed, e.g. ["MLS_B B7"]. */
  possibleDuplicates: string[]
}

export interface SearchPage {
  items: ScoredListing[]
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

/** What the user typed into the form. Strings, because that is what inputs hold; '' means not supplied. */
export interface SearchFilters {
  minPrice: string
  maxPrice: string
  minBedrooms: string
  city: string
  keyword: string
  /** '' (any), 'active', 'pending' or 'sold'. */
  status: string
  targetBudget: string
}

/** The form's starting state: no filters. */
export const EMPTY_FILTERS: SearchFilters = {
  minPrice: '',
  maxPrice: '',
  minBedrooms: '',
  city: '',
  keyword: '',
  status: '',
  targetBudget: '',
}

/** A full request: the filters plus which page to fetch. */
export interface SearchParams extends SearchFilters {
  page: number
  pageSize: number
}
