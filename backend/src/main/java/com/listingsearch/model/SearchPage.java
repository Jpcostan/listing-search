package com.listingsearch.model;

import java.util.List;

/** One page of ranked results plus the numbers the UI needs to draw page controls. */
public record SearchPage(
        List<ScoredListing> items,
        int page,
        int pageSize,
        int totalItems,
        int totalPages) {
}
