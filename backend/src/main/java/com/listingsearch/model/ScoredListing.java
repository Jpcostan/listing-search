package com.listingsearch.model;

import java.util.List;

/**
 * A listing paired with its relevance score (0 to 1, higher is better).
 * possibleDuplicates names other listings that look like the same property from another feed,
 * e.g. ["MLS_B B7"]. Empty when there are none.
 */
public record ScoredListing(Listing listing, double score, List<String> possibleDuplicates) {
}
