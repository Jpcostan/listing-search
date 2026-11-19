package com.listingsearch;

import java.time.LocalDate;

import com.listingsearch.model.Listing;

/** Builds listings for tests. Only the fields a test cares about are parameters; the rest are fixed. */
public final class TestListings {

    private TestListings() {
    }

    /** A listing with a unique location (derived from the id) so it is nobody's duplicate. */
    public static Listing listing(
            String id, String source, String city, double price, int bedrooms, String listedDate, String description) {
        double uniqueLatitude = 38.0 + Math.abs((source + id).hashCode() % 10_000) / 10_000.0;
        return listingAt(id, source, city, price, bedrooms, listedDate, description, uniqueLatitude, -77.0);
    }

    /** A listing where only the status matters. */
    public static Listing listingWithStatus(String id, String status) {
        Listing base = listing(id, "MLS_A", "Fairfax", 400_000, 2, "2026-11-01", "");
        return new Listing(
                base.id(), base.source(), base.address(), base.city(), base.state(), base.zip(), base.price(),
                base.bedrooms(), base.bathrooms(), base.sqft(), base.latitude(), base.longitude(),
                base.listedDate(), status, base.description());
    }

    /** A listing at an exact location, for duplicate tests. */
    public static Listing listingAt(
            String id, String source, String city, double price, int bedrooms, String listedDate, String description,
            double latitude, double longitude) {
        return new Listing(
                id, source, "1 Test St", city, "VA", "22000", price, bedrooms, 1.0, 1000,
                latitude, longitude, LocalDate.parse(listedDate), "active", description);
    }
}
