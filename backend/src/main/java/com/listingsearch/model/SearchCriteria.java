package com.listingsearch.model;

/**
 * A search request that has already passed validation.
 * Filters are null when the user did not supply them. page and pageSize always have a value.
 */
public record SearchCriteria(
        Double minPrice,
        Double maxPrice,
        Integer minBedrooms,
        String city,
        String keyword,
        String status,
        Double targetBudget,
        int page,
        int pageSize) {
}
