package com.listingsearch.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.listingsearch.model.SearchCriteria;
import com.listingsearch.model.SearchPage;
import com.listingsearch.service.ListingSearchService;
import com.listingsearch.service.SearchValidator;

/** The one HTTP endpoint. Reads query parameters, validates, searches, returns JSON. */
@RestController
public class ListingSearchController {

    private final SearchValidator validator;
    private final ListingSearchService searchService;

    public ListingSearchController(SearchValidator validator, ListingSearchService searchService) {
        this.validator = validator;
        this.searchService = searchService;
    }

    /** GET /api/listings/search. Every parameter is optional. */
    @GetMapping("/api/listings/search")
    public SearchPage search(
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double targetBudget,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        SearchCriteria criteria = validator.validate(
                minPrice, maxPrice, minBedrooms, city, keyword, status, targetBudget, page, pageSize);
        return searchService.search(criteria);
    }
}
