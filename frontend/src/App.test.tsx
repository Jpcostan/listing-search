import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { jsonResponse, scoredListing, searchPage } from './testData'

// Every test replaces the browser's fetch with this fake, so no real backend is needed.
const fetchMock = vi.fn()

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

/** The URL of the most recent request the app made. */
function lastRequestUrl(): string {
  return fetchMock.mock.calls[fetchMock.mock.calls.length - 1][0]
}

describe('App', () => {
  it('shows a loading message while the request is in flight', () => {
    fetchMock.mockReturnValue(new Promise(() => {})) // never finishes

    render(<App />)

    expect(screen.getByText('Loading...')).toBeInTheDocument()
  })

  it('renders address, price, bedrooms and score for each result', async () => {
    fetchMock.mockResolvedValue(jsonResponse(searchPage([scoredListing('A1', '123 Main St')])))

    render(<App />)

    expect(await screen.findByText('123 Main St')).toBeInTheDocument()
    expect(screen.getByText('$450,000')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('0.875')).toBeInTheDocument()
    expect(screen.queryByText('Loading...')).not.toBeInTheDocument()
  })

  it('shows a possible-duplicate note only on listings that have one', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(searchPage([scoredListing('A1', '123 Main St', ['MLS_B B7']), scoredListing('A2', '456 Oak Ave')])),
    )

    render(<App />)

    expect(await screen.findByText('Possible duplicate of MLS_B B7')).toBeInTheDocument()
    expect(screen.getAllByText(/Possible duplicate/)).toHaveLength(1)
  })

  it('shows a no-results message when the page is empty', async () => {
    fetchMock.mockResolvedValue(jsonResponse(searchPage([], 1, 0)))

    render(<App />)

    expect(await screen.findByText('No listings match your search.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('shows the API error message when the request fails', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ error: 'pageSize must be between 1 and 100' }, 400))

    render(<App />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Error: pageSize must be between 1 and 100')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('Next requests the next page and Previous is disabled on page 1', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(searchPage([scoredListing('A1', '123 Main St')], 1, 2)))
    fetchMock.mockResolvedValueOnce(jsonResponse(searchPage([scoredListing('A2', '456 Oak Ave')], 2, 2)))

    render(<App />)

    expect(await screen.findByText(/Page 1 of 2/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Next' }))

    expect(await screen.findByText('456 Oak Ave')).toBeInTheDocument()
    expect(lastRequestUrl()).toContain('page=2')
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeEnabled()
  })

  it('sends the form values to the API', async () => {
    fetchMock.mockImplementation(async () => jsonResponse(searchPage([scoredListing('A1', '123 Main St')])))

    render(<App />)
    await screen.findByText('123 Main St')

    await userEvent.type(screen.getByLabelText('Max price'), '600000')
    await userEvent.type(screen.getByLabelText('City'), 'Reston')
    await userEvent.type(screen.getByLabelText('Target budget'), '550000')
    await userEvent.selectOptions(screen.getByLabelText('Status'), 'active')
    await userEvent.click(screen.getByRole('button', { name: 'Search' }))

    await screen.findByText('123 Main St')
    expect(lastRequestUrl()).toBe(
      '/api/listings/search?maxPrice=600000&city=Reston&status=active&targetBudget=550000&page=1&pageSize=10',
    )
  })

  it('submitting the form resets to page 1', async () => {
    fetchMock.mockImplementation(async (url: string) => {
      const page = url.includes('page=2') ? 2 : 1
      return jsonResponse(searchPage([scoredListing(`A${page}`, `${page} Test St`)], page, 2))
    })

    render(<App />)
    await userEvent.click(await screen.findByRole('button', { name: 'Next' }))
    expect(await screen.findByText(/Page 2 of 2/)).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Keyword'), 'pool')
    await userEvent.click(screen.getByRole('button', { name: 'Search' }))

    expect(await screen.findByText(/Page 1 of 2/)).toBeInTheDocument()
    expect(lastRequestUrl()).toContain('keyword=pool')
    expect(lastRequestUrl()).toContain('page=1')
  })
})
