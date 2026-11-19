package com.listingsearch.model;

import java.time.LocalDate;

/** One property listing, exactly as it appears in the feed JSON. */
public record Listing(
        String id,
        String source,
        String address,
        String city,
        String state,
        String zip,
        double price,
        int bedrooms,
        double bathrooms,
        int sqft,
        double latitude,
        double longitude,
        LocalDate listedDate,
        String status,
        String description) {
}
