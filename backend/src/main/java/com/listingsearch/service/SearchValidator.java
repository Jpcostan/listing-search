package com.listingsearch.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.listingsearch.model.Listing;
import com.listingsearch.model.SearchCriteria;
import com.listingsearch.repository.ListingRepository;

/** Checks raw request values and turns them into a SearchCriteria, or throws ValidationException. */
@Component
public class SearchValidator {

    static final int DEFAULT_PAGE = 1;
    static final int DEFAULT_PAGE_SIZE = 10;
    static final int MAX_PAGE_SIZE = 100;

    /** The statuses the handout's data model allows. */
    static final List<String> KNOWN_STATUSES = List.of("active", "pending", "sold");

    private final ListingRepository repository;

    public SearchValidator(ListingRepository repository) {
        this.repository = repository;
    }

    /**
     * Rules, each with its own clear message:
     * - minPrice, maxPrice, minBedrooms must not be negative
     * - minPrice must not be greater than maxPrice
     * - targetBudget must be greater than 0
     * - page must be 1 or more (default 1)
     * - pageSize must be 1 to 100 (default 10)
     * - city must match a city we have listings for (case-insensitive); the error lists known cities
     * - status must be active, pending or sold (case-insensitive)
     * - blank city, keyword or status is treated as "not supplied"
     */
    public SearchCriteria validate(
            Double minPrice,
            Double maxPrice,
            Integer minBedrooms,
            String city,
            String keyword,
            String status,
            Double targetBudget,
            Integer page,
            Integer pageSize) {

        requireZeroOrMore("minPrice", minPrice);
        requireZeroOrMore("maxPrice", maxPrice);

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new ValidationException("minPrice must not be greater than maxPrice");
        }
        if (minBedrooms != null && minBedrooms < 0) {
            throw new ValidationException("minBedrooms must be 0 or greater");
        }
        if (targetBudget != null && (!Double.isFinite(targetBudget) || targetBudget <= 0)) {
            throw new ValidationException("targetBudget must be greater than 0");
        }

        int pageOrDefault = (page == null) ? DEFAULT_PAGE : page;
        if (pageOrDefault < 1) {
            throw new ValidationException("page must be 1 or greater");
        }

        int pageSizeOrDefault = (pageSize == null) ? DEFAULT_PAGE_SIZE : pageSize;
        if (pageSizeOrDefault < 1 || pageSizeOrDefault > MAX_PAGE_SIZE) {
            throw new ValidationException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }

        String cityOrNull = blankToNull(city);
        if (cityOrNull != null) {
            cityOrNull = findKnownCity(cityOrNull);
        }

        String statusOrNull = blankToNull(status);
        if (statusOrNull != null) {
            statusOrNull = statusOrNull.toLowerCase();
            if (!KNOWN_STATUSES.contains(statusOrNull)) {
                throw new ValidationException(
                        "Unknown status '" + status.trim() + "'. Known statuses: " + String.join(", ", KNOWN_STATUSES));
            }
        }

        return new SearchCriteria(
                minPrice,
                maxPrice,
                minBedrooms,
                cityOrNull,
                blankToNull(keyword),
                statusOrNull,
                targetBudget,
                pageOrDefault,
                pageSizeOrDefault);
    }

    /** Rejects negative numbers, and NaN/Infinity (which "?minPrice=NaN" would otherwise let through). */
    private void requireZeroOrMore(String name, Double value) {
        if (value != null && (!Double.isFinite(value) || value < 0)) {
            throw new ValidationException(name + " must be 0 or greater");
        }
    }

    /** Trims the text; returns null if nothing is left. */
    private String blankToNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    /** Returns the city spelled the way the data spells it, or throws listing the cities we do have. */
    private String findKnownCity(String city) {
        List<String> knownCities = repository.findAll().stream()
                .map(Listing::city)
                .distinct()
                .sorted()
                .toList();

        for (String known : knownCities) {
            if (known.equalsIgnoreCase(city)) {
                return known;
            }
        }
        throw new ValidationException(
                "Unknown city '" + city + "'. Known cities: " + String.join(", ", knownCities));
    }
}
