# Listing Search Service

A small full-stack search over property listings from two MLS feeds.

- **Backend:** Java 21, Spring Boot 3, Maven. One REST endpoint, listings held in memory.
- **Frontend:** React + TypeScript (Vite). A search form, a results table, and page controls.

## Running it

Requires Java 21, Maven, and Node 20.19 or newer.

```bash
# Terminal 1: backend on http://localhost:8080
cd backend
mvn spring-boot:run

# Terminal 2: frontend on http://localhost:5173
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The Vite dev server forwards `/api` requests to the backend, so no
CORS configuration is needed.

## Running the tests

```bash
cd backend && mvn test      # 61 tests (JUnit 5)
cd frontend && npm test     # 13 tests (Vitest + React Testing Library)
```

## API

`GET /api/listings/search`

| Parameter | Meaning | Default |
|---|---|---|
| `minPrice`, `maxPrice` | Inclusive price range | none |
| `minBedrooms` | Minimum bedrooms | none |
| `city` | Exact city, case-insensitive | none |
| `keyword` | Case-insensitive text contained in the description | none |
| `status` | `active`, `pending` or `sold` | any status |
| `targetBudget` | Used for ranking only; never removes a listing | none |
| `page` | 1-based page number | 1 |
| `pageSize` | 1 to 100 | 10 |

A successful response has `items` (each with `listing`, `score`, `possibleDuplicates`), `page`,
`pageSize`, `totalItems` and `totalPages`. Every error is `{"error": "message"}`.

## How a search works

`ListingSearchService.search` runs four separate steps, each its own method:

1. **Filter**: keep listings that pass every supplied filter.
2. **Score**: give each one a relevance score.
3. **Sort**: highest score first.
4. **Paginate**: cut out the requested page.

## Scoring

```
score = 0.7 * budgetFit + 0.3 * recency        (rounded to 3 decimals, range 0 to 1)
```

- **budgetFit** = `max(0, 1 - |price - targetBudget| / targetBudget)`.
  A listing exactly on budget scores 1. It falls in a straight line to 0 at double the budget.
  It is symmetric on purpose: a home far *below* budget is usually a different kind of
  property, not a better match.
- **recency** = `max(0, 1 - daysSinceListed / 90)`.
  Listed today scores 1, falling in a straight line to 0 at 90 days. After roughly 90 days on
  the market a listing is generally considered stale, so it gets no credit for being new.
- With **no targetBudget**, the score is recency alone.
- **Ties** are broken by newest `listedDate`, then `source`, then `id`, so the order is always
  the same for the same request.

"Today" comes from an injected `Clock`, so tests can freeze the date.

### Scoring trade-offs

- **Fixed weights.** 0.7 / 0.3 says budget matters more than freshness. They are named
  constants, not user-tunable.
- **Linear decay with a hard cutoff.** Simple to explain and predict, but every listing older
  than 90 days scores the same 0 for recency (the tie-break still orders those by date).
- **Budget is symmetric.** A user who would love to pay less might prefer an asymmetric curve.

## Invalid input

Invalid input returns HTTP 400 with a specific message, never a crash or a silent empty result:

| Input | Result |
|---|---|
| `minPrice` greater than `maxPrice` | 400 |
| Negative price or bedrooms, `targetBudget` of 0 or less | 400 |
| `pageSize` below 1 or above 100, `page` below 1 | 400 |
| `page` past the last page | 400, message says how many pages exist |
| Non-numeric value such as `minPrice=abc` | 400, message names the parameter |
| A `city` we have no listings for | 400, message lists the known cities |
| A `status` other than active, pending, sold | 400, message lists the valid ones |
| A `keyword` (or other valid filters) matching nothing | 200 with an empty page |

The last row is deliberate: an unknown city is most likely a typo, so it is reported as an
error, while a valid search that happens to match nothing is a normal "no results" outcome.

## Data

`backend/src/main/resources/listings.json` holds the 12 listings from the provided
`sample_listings.json`, unchanged, plus **8 listings I added** (ids A8 to A11 and B12 to B15,
listed 2026-09-18 to 2026-11-17). Adding data was confirmed as acceptable beforehand. The
original listings are all from July to early September 2026, so with a 90-day window measured
from today they score close to 0 for recency; the added listings give the recency half of the
score something to work with. Because recency is measured from today's date, all scores decay
as time passes.

## Possible duplicates

The same property can arrive from both feeds with slightly different address text and price
(for example `A1` "123 Main St, Apt 4B" and `B7` "123 Main Street, Unit 4B"). The service does
not merge or hide these. Instead each result carries `possibleDuplicates`, and the UI shows a
"Possible duplicate of MLS_B B7" note.

- Two listings are treated as possible duplicates when their latitude and longitude are
  identical. Address text is not compared because it is not normalized between feeds.
- The check runs against all listings, so a duplicate is still flagged when its twin is on
  another page or was filtered out.
- Limitation: two different units in one building would share coordinates and be flagged.

## Other trade-offs

- **In-memory data, no database.** Fine for a small fixed dataset; every search scans the full
  list, and duplicate detection compares each result with every listing.
- **`status` was added beyond the brief** as an optional filter. With no status chosen, all
  statuses are shown, including pending.
- **Keyword search is a plain "contains"**, so "pet" also matches "No pets".

## What I would add next

- Merge cross-source duplicates into one result, with a rule for which record wins.
- Normalize addresses and compare unit numbers for better duplicate detection.
- Sort options and user-adjustable weights.
- A database once the data no longer fits comfortably in memory.
