import type { ScoredListing } from '../types'

interface ResultsTableProps {
  items: ScoredListing[]
}

/**
 * A table of the current page: address, city, price, bedrooms, status, listed date, source, relevance score.
 * A final Notes column shows "Possible duplicate of MLS_B B7" when the listing has possibleDuplicates.
 */
export function ResultsTable({ items }: ResultsTableProps) {
  return (
    <table border={1} cellPadding={6}>
      <thead>
        <tr>
          <th>Address</th>
          <th>City</th>
          <th>Price</th>
          <th>Bedrooms</th>
          <th>Status</th>
          <th>Listed</th>
          <th>Source</th>
          <th>Relevance</th>
          <th>Notes</th>
        </tr>
      </thead>
      <tbody>
        {items.map(({ listing, score, possibleDuplicates }) => (
          // ids are only unique within one source, so the key needs both
          <tr key={`${listing.source}-${listing.id}`}>
            <td>{listing.address}</td>
            <td>{listing.city}</td>
            <td>${listing.price.toLocaleString('en-US')}</td>
            <td>{listing.bedrooms}</td>
            <td>{listing.status}</td>
            <td>{listing.listedDate}</td>
            <td>
              {listing.source} {listing.id}
            </td>
            <td>{score.toFixed(3)}</td>
            <td>{possibleDuplicates.length > 0 && `Possible duplicate of ${possibleDuplicates.join(', ')}`}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
