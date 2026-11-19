package com.listingsearch.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listingsearch.model.Listing;

/** Holds every listing in memory. Reads listings.json once at startup. */
@Repository
public class ListingRepository {

    private final List<Listing> listings;

    /** Used by Spring: loads listings.json from the classpath using Spring's JSON mapper. */
    @Autowired
    public ListingRepository(ObjectMapper objectMapper) throws IOException {
        try (InputStream json = new ClassPathResource("listings.json").getInputStream()) {
            this.listings = List.of(objectMapper.readValue(json, Listing[].class));
        }
    }

    /** Used by tests: holds exactly the listings it is given. */
    public ListingRepository(List<Listing> listings) {
        this.listings = List.copyOf(listings);
    }

    /** Returns all listings, unfiltered. The list cannot be modified. */
    public List<Listing> findAll() {
        return listings;
    }
}
