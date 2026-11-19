import { useState } from 'react'
import { EMPTY_FILTERS } from '../types'
import type { SearchFilters } from '../types'

interface SearchFormProps {
  /** Called with the form's values when the user presses Search. */
  onSearch: (filters: SearchFilters) => void
}

/**
 * Inputs for minPrice, maxPrice, minBedrooms, city, keyword, targetBudget,
 * a status dropdown (Any / Active / Pending / Sold), and a Search button.
 */
export function SearchForm({ onSearch }: SearchFormProps) {
  const [filters, setFilters] = useState<SearchFilters>(EMPTY_FILTERS)

  /** Replaces one field, keeping the others. */
  function update(name: keyof SearchFilters, value: string) {
    setFilters({ ...filters, [name]: value })
  }

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault() // stop the browser from reloading the page
        onSearch(filters)
      }}
    >
      <label>
        Min price{' '}
        <input type="number" value={filters.minPrice} onChange={(e) => update('minPrice', e.target.value)} />
      </label>{' '}
      <label>
        Max price{' '}
        <input type="number" value={filters.maxPrice} onChange={(e) => update('maxPrice', e.target.value)} />
      </label>{' '}
      <label>
        Min bedrooms{' '}
        <input type="number" value={filters.minBedrooms} onChange={(e) => update('minBedrooms', e.target.value)} />
      </label>
      <br />
      <label>
        City <input type="text" value={filters.city} onChange={(e) => update('city', e.target.value)} />
      </label>{' '}
      <label>
        Keyword <input type="text" value={filters.keyword} onChange={(e) => update('keyword', e.target.value)} />
      </label>{' '}
      <label>
        Status{' '}
        <select value={filters.status} onChange={(e) => update('status', e.target.value)}>
          <option value="">Any</option>
          <option value="active">Active</option>
          <option value="pending">Pending</option>
          <option value="sold">Sold</option>
        </select>
      </label>
      <br />
      <label>
        Target budget{' '}
        <input type="number" value={filters.targetBudget} onChange={(e) => update('targetBudget', e.target.value)} />
      </label>{' '}
      <button type="submit">Search</button>
    </form>
  )
}
