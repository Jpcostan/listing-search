package com.listingsearch.service;

import static com.listingsearch.TestListings.listing;
import static com.listingsearch.TestListings.listingAt;
import static com.listingsearch.TestListings.listingWithStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.listingsearch.model.Listing;
import com.listingsearch.model.ScoredListing;
import com.listingsearch.model.SearchCriteria;
import com.listingsearch.model.SearchPage;
import com.listingsearch.repository.ListingRepository;

class ListingSearchServiceTest {

    /** "Today" is frozen at 2026-11-19 so recency scores never change between runs. */
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC);

    /** Five listings, newest first, so with no targetBudget the expected order is A1..A5. */
    private static final List<Listing> FIVE_LISTINGS = List.of(
            listing("A1", "MLS_A", "Fairfax", 400_000, 2, "2026-11-18", "Cozy condo with a pool"),
            listing("A2", "MLS_A", "Fairfax", 500_000, 3, "2026-11-10", "Fenced yard, pets allowed"),
            listing("A3", "MLS_A", "Reston", 600_000, 4, "2026-11-01", "Large home with POOL and garage"),
            listing("A4", "MLS_A", "Reston", 700_000, 4, "2026-10-20", "Quiet street"),
            listing("A5", "MLS_A", "Vienna", 800_000, 5, "2026-10-01", "Walk to Metro"));

    private static ListingSearchService serviceWith(List<Listing> listings) {
        return new ListingSearchService(new ListingRepository(listings), new RelevanceScorer(), FIXED_CLOCK);
    }

    /** Criteria with only the given filters set, page 1, page size 10. */
    private static SearchCriteria criteria(
            Double minPrice, Double maxPrice, Integer minBedrooms, String city, String keyword, Double targetBudget) {
        return new SearchCriteria(minPrice, maxPrice, minBedrooms, city, keyword, null, targetBudget, 1, 10);
    }

    private static SearchCriteria noFiltersPage(int page, int pageSize) {
        return new SearchCriteria(null, null, null, null, null, null, null, page, pageSize);
    }

    private static SearchCriteria statusOnly(String status) {
        return new SearchCriteria(null, null, null, null, null, status, null, 1, 10);
    }

    private static List<String> ids(SearchPage page) {
        return page.items().stream().map(item -> item.listing().id()).toList();
    }

    // filtering

    @Test
    void noFiltersReturnsEveryListing() {
        SearchPage page = serviceWith(FIVE_LISTINGS).search(criteria(null, null, null, null, null, null));

        assertThat(ids(page)).containsExactly("A1", "A2", "A3", "A4", "A5");
        assertThat(page.totalItems()).isEqualTo(5);
    }

    @Test
    void minAndMaxPriceAreInclusive() {
        SearchPage page = serviceWith(FIVE_LISTINGS).search(criteria(500_000.0, 700_000.0, null, null, null, null));

        assertThat(ids(page)).containsExactly("A2", "A3", "A4");
    }

    @Test
    void minBedroomsFilters() {
        SearchPage page = serviceWith(FIVE_LISTINGS).search(criteria(null, null, 4, null, null, null));

        assertThat(ids(page)).containsExactly("A3", "A4", "A5");
    }

    @Test
    void cityFilters() {
        SearchPage page = serviceWith(FIVE_LISTINGS).search(criteria(null, null, null, "Reston", null, null));

        assertThat(ids(page)).containsExactly("A3", "A4");
    }

    @Test
    void keywordMatchesDescriptionIgnoringCase() {
        SearchPage page = serviceWith(FIVE_LISTINGS).search(criteria(null, null, null, null, "pool", null));

        assertThat(ids(page)).containsExactly("A1", "A3");
    }

    @Test
    void filtersCombine() {
        SearchPage page = serviceWith(FIVE_LISTINGS).search(criteria(450_000.0, null, 4, "Reston", "pool", null));

        assertThat(ids(page)).containsExactly("A3");
    }

    @Test
    void keywordWithNoMatchesReturnsEmptyPage() {
        SearchPage page = serviceWith(FIVE_LISTINGS).search(criteria(null, null, null, null, "helipad", null));

        assertThat(page.items()).isEmpty();
        assertThat(page.totalItems()).isEqualTo(0);
        assertThat(page.totalPages()).isEqualTo(0);
        assertThat(page.page()).isEqualTo(1);
    }

    @Test
    void statusFilters() {
        List<Listing> listings = List.of(
                listingWithStatus("A1", "active"),
                listingWithStatus("A2", "pending"),
                listingWithStatus("A3", "active"));

        assertThat(ids(serviceWith(listings).search(statusOnly("pending")))).containsExactly("A2");
        assertThat(ids(serviceWith(listings).search(statusOnly("active")))).containsExactlyInAnyOrder("A1", "A3");
    }

    @Test
    void noStatusReturnsEveryStatus() {
        List<Listing> listings = List.of(listingWithStatus("A1", "active"), listingWithStatus("A2", "pending"));

        assertThat(serviceWith(listings).search(statusOnly(null)).totalItems()).isEqualTo(2);
    }

    @Test
    void knownStatusWithNoListingsReturnsEmptyPage() {
        List<Listing> listings = List.of(listingWithStatus("A1", "active"));

        assertThat(serviceWith(listings).search(statusOnly("sold")).items()).isEmpty();
    }

    // ranking

    @Test
    void resultsAreOrderedByScoreDescending() {
        // With a 700,000 budget, A4 is an exact match and beats the newer listings.
        SearchPage page = serviceWith(FIVE_LISTINGS).search(criteria(null, null, null, null, null, 700_000.0));

        assertThat(ids(page).get(0)).isEqualTo("A4");
        List<Double> scores = page.items().stream().map(ScoredListing::score).toList();
        assertThat(scores).isSortedAccordingTo((a, b) -> Double.compare(b, a));
    }

    @Test
    void tiedScoresAreOrderedByListedDateThenSourceThenId() {
        // All four are more than 90 days old, so every score is 0 and only the tie-breakers decide.
        List<Listing> tied = List.of(
                listing("A1", "MLS_B", "Fairfax", 400_000, 2, "2026-06-01", ""),
                listing("A0", "MLS_B", "Fairfax", 400_000, 2, "2026-06-01", ""),
                listing("A1", "MLS_A", "Fairfax", 400_000, 2, "2026-06-01", ""),
                listing("Z9", "MLS_B", "Fairfax", 400_000, 2, "2026-06-02", ""));

        SearchPage page = serviceWith(tied).search(criteria(null, null, null, null, null, null));

        assertThat(page.items()).allMatch(item -> item.score() == 0.0);
        List<String> order = page.items().stream()
                .map(item -> item.listing().source() + " " + item.listing().id())
                .toList();
        assertThat(order).containsExactly(
                "MLS_B Z9",   // newest listedDate wins
                "MLS_A A1",   // same date: source A before B
                "MLS_B A0",   // same date and source: id A0 before A1
                "MLS_B A1");
    }

    // possible duplicates

    @Test
    void listingsAtTheSameCoordinatesFlagEachOther() {
        List<Listing> listings = List.of(
                listingAt("A1", "MLS_A", "Fairfax", 450_000, 2, "2026-11-10", "", 38.5, -77.5),
                listingAt("B7", "MLS_B", "Fairfax", 452_000, 2, "2026-11-08", "", 38.5, -77.5));

        SearchPage page = serviceWith(listings).search(criteria(null, null, null, null, null, null));

        assertThat(page.items().get(0).listing().id()).isEqualTo("A1");
        assertThat(page.items().get(0).possibleDuplicates()).containsExactly("MLS_B B7");
        assertThat(page.items().get(1).possibleDuplicates()).containsExactly("MLS_A A1");
    }

    @Test
    void listingWithNoTwinHasNoPossibleDuplicates() {
        SearchPage page = serviceWith(FIVE_LISTINGS).search(criteria(null, null, null, null, null, null));

        assertThat(page.items()).allMatch(item -> item.possibleDuplicates().isEmpty());
    }

    @Test
    void duplicateIsFlaggedEvenWhenTheTwinIsFilteredOut() {
        List<Listing> listings = List.of(
                listingAt("A1", "MLS_A", "Fairfax", 450_000, 2, "2026-11-10", "", 38.5, -77.5),
                listingAt("B7", "MLS_B", "Fairfax", 452_000, 2, "2026-11-08", "", 38.5, -77.5));

        // maxPrice keeps A1 and removes B7 from the results.
        SearchPage page = serviceWith(listings).search(criteria(null, 451_000.0, null, null, null, null));

        assertThat(ids(page)).containsExactly("A1");
        assertThat(page.items().get(0).possibleDuplicates()).containsExactly("MLS_B B7");
    }

    @Test
    void flaggedDuplicatesAreNotRemovedFromResults() {
        List<Listing> listings = List.of(
                listingAt("A1", "MLS_A", "Fairfax", 450_000, 2, "2026-11-10", "", 38.5, -77.5),
                listingAt("B7", "MLS_B", "Fairfax", 452_000, 2, "2026-11-08", "", 38.5, -77.5));

        SearchPage page = serviceWith(listings).search(criteria(null, null, null, null, null, null));

        assertThat(page.totalItems()).isEqualTo(2);
    }

    // pagination

    @Test
    void firstPageHasPageSizeItems() {
        SearchPage page = serviceWith(FIVE_LISTINGS).search(noFiltersPage(1, 2));

        assertThat(ids(page)).containsExactly("A1", "A2");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.pageSize()).isEqualTo(2);
    }

    @Test
    void lastPageHasTheRemainder() {
        SearchPage page = serviceWith(FIVE_LISTINGS).search(noFiltersPage(3, 2));

        assertThat(ids(page)).containsExactly("A5");
    }

    @Test
    void totalPagesRoundsUp() {
        assertThat(serviceWith(FIVE_LISTINGS).search(noFiltersPage(1, 2)).totalPages()).isEqualTo(3);
        assertThat(serviceWith(FIVE_LISTINGS).search(noFiltersPage(1, 5)).totalPages()).isEqualTo(1);
        assertThat(serviceWith(FIVE_LISTINGS).search(noFiltersPage(1, 100)).totalPages()).isEqualTo(1);
    }

    @Test
    void pageBeyondLastIsRejected() {
        assertThatThrownBy(() -> serviceWith(FIVE_LISTINGS).search(noFiltersPage(4, 2)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("page 4 is past the last page (total pages: 3)");
    }

    @Test
    void pageOneOfEmptyResultIsAllowed() {
        SearchPage page = serviceWith(List.of()).search(noFiltersPage(1, 10));

        assertThat(page.items()).isEmpty();
        assertThat(page.totalPages()).isEqualTo(0);
    }

    @Test
    void pageTwoOfEmptyResultIsRejected() {
        assertThatThrownBy(() -> serviceWith(List.of()).search(noFiltersPage(2, 10)))
                .isInstanceOf(ValidationException.class);
    }
}
