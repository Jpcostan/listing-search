package com.listingsearch.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.listingsearch.model.Listing;
import com.listingsearch.model.ScoredListing;
import com.listingsearch.model.SearchCriteria;
import com.listingsearch.model.SearchPage;
import com.listingsearch.repository.ListingRepository;

/** Runs a search as four separate steps: filter, score, sort, paginate. */
@Service
public class ListingSearchService {

    private final ListingRepository repository;
    private final RelevanceScorer scorer;
    private final Clock clock;

    public ListingSearchService(ListingRepository repository, RelevanceScorer scorer, Clock clock) {
        this.repository = repository;
        this.scorer = scorer;
        this.clock = clock;
    }

    /** Calls the four steps below in order and returns the requested page. */
    public SearchPage search(SearchCriteria criteria) {
        List<Listing> matching = filter(repository.findAll(), criteria);
        List<ScoredListing> scored = score(matching, criteria.targetBudget());
        List<ScoredListing> sorted = sort(scored);
        return paginate(sorted, criteria.page(), criteria.pageSize());
    }

    /** Keeps listings that pass every supplied filter. Keyword is a case-insensitive "contains" on description. */
    List<Listing> filter(List<Listing> listings, SearchCriteria criteria) {
        List<Listing> matching = new ArrayList<>();
        for (Listing listing : listings) {
            if (matches(listing, criteria)) {
                matching.add(listing);
            }
        }
        return matching;
    }

    /** True if the listing passes every filter the user supplied. A null filter is skipped. */
    private boolean matches(Listing listing, SearchCriteria criteria) {
        if (criteria.minPrice() != null && listing.price() < criteria.minPrice()) {
            return false;
        }
        if (criteria.maxPrice() != null && listing.price() > criteria.maxPrice()) {
            return false;
        }
        if (criteria.minBedrooms() != null && listing.bedrooms() < criteria.minBedrooms()) {
            return false;
        }
        if (criteria.city() != null && !listing.city().equalsIgnoreCase(criteria.city())) {
            return false;
        }
        if (criteria.keyword() != null
                && !listing.description().toLowerCase().contains(criteria.keyword().toLowerCase())) {
            return false;
        }
        if (criteria.status() != null && !listing.status().equalsIgnoreCase(criteria.status())) {
            return false;
        }
        return true;
    }

    /** Attaches a relevance score and any possible duplicates to each listing. */
    List<ScoredListing> score(List<Listing> listings, Double targetBudget) {
        LocalDate today = LocalDate.now(clock);
        List<Listing> allListings = repository.findAll();

        List<ScoredListing> scored = new ArrayList<>();
        for (Listing listing : listings) {
            double score = scorer.score(listing, targetBudget, today);
            List<String> possibleDuplicates = findPossibleDuplicates(listing, allListings);
            scored.add(new ScoredListing(listing, score, possibleDuplicates));
        }
        return scored;
    }

    /**
     * Finds other listings at the same latitude and longitude, returned as "SOURCE ID" labels.
     * Looks through ALL listings, not just the filtered ones, so a duplicate is still flagged
     * when its twin is on another page or was filtered out. Nothing is removed from the results.
     */
    List<String> findPossibleDuplicates(Listing listing, List<Listing> allListings) {
        List<String> labels = new ArrayList<>();
        for (Listing other : allListings) {
            boolean isSameRecord = other.source().equals(listing.source()) && other.id().equals(listing.id());
            boolean isSameLocation = other.latitude() == listing.latitude()
                    && other.longitude() == listing.longitude();

            if (!isSameRecord && isSameLocation) {
                labels.add(other.source() + " " + other.id());
            }
        }
        return labels;
    }

    /** Highest score first. Ties: newest listedDate, then source, then id, so order is always the same. */
    List<ScoredListing> sort(List<ScoredListing> scored) {
        Comparator<ScoredListing> highestScoreFirst =
                Comparator.comparingDouble(ScoredListing::score).reversed();
        Comparator<ScoredListing> newestFirst =
                Comparator.comparing((ScoredListing s) -> s.listing().listedDate()).reversed();
        Comparator<ScoredListing> bySource = Comparator.comparing(s -> s.listing().source());
        Comparator<ScoredListing> byId = Comparator.comparing(s -> s.listing().id());

        List<ScoredListing> sorted = new ArrayList<>(scored);
        sorted.sort(highestScoreFirst.thenComparing(newestFirst).thenComparing(bySource).thenComparing(byId));
        return sorted;
    }

    /** Cuts out one page. Throws ValidationException if the page is past the last page. */
    SearchPage paginate(List<ScoredListing> sorted, int page, int pageSize) {
        int totalItems = sorted.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        // Page 1 is always allowed, so "no results" is an empty page rather than an error.
        if (page > 1 && page > totalPages) {
            throw new ValidationException(
                    "page " + page + " is past the last page (total pages: " + totalPages + ")");
        }

        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, totalItems);
        return new SearchPage(sorted.subList(from, to), page, pageSize, totalItems, totalPages);
    }
}
