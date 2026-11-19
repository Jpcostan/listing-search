package com.listingsearch.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full HTTP round trip through MockMvc: starts the whole app and loads the real listings.json.
 * Assertions only rely on the file being non-empty, not on specific listings in it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ListingSearchControllerTest {

    private static final String SEARCH = "/api/listings/search";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchWithNoParametersReturns200AndFirstPage() throws Exception {
        mockMvc.perform(get(SEARCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.items", not(empty())))
                .andExpect(jsonPath("$.totalItems").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void responseContainsAddressPriceBedroomsAndScore() throws Exception {
        mockMvc.perform(get(SEARCH).param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].listing.address").isString())
                .andExpect(jsonPath("$.items[0].listing.price").isNumber())
                .andExpect(jsonPath("$.items[0].listing.bedrooms").isNumber())
                .andExpect(jsonPath("$.items[0].listing.listedDate").isString())
                .andExpect(jsonPath("$.items[0].score").isNumber())
                .andExpect(jsonPath("$.items[0].possibleDuplicates").isArray());
    }

    @Test
    void minPriceGreaterThanMaxPriceReturns400WithErrorBody() throws Exception {
        mockMvc.perform(get(SEARCH).param("minPrice", "500000").param("maxPrice", "400000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("minPrice must not be greater than maxPrice"));
    }

    @Test
    void pageSizeZeroReturns400WithErrorBody() throws Exception {
        mockMvc.perform(get(SEARCH).param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("pageSize must be between 1 and 100"));
    }

    @Test
    void unknownCityReturns400WithErrorBody() throws Exception {
        mockMvc.perform(get(SEARCH).param("city", "Atlantis"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Unknown city 'Atlantis'. Known cities: ")));
    }

    @Test
    void unknownStatusReturns400WithErrorBody() throws Exception {
        mockMvc.perform(get(SEARCH).param("status", "haunted"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown status 'haunted'. Known statuses: active, pending, sold"));
    }

    @Test
    void validStatusReturns200() throws Exception {
        mockMvc.perform(get(SEARCH).param("status", "Active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())));
    }

    @Test
    void nonNumericPriceReturns400WithErrorBody() throws Exception {
        mockMvc.perform(get(SEARCH).param("minPrice", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("minPrice must be a number, but was 'abc'"));
    }

    @Test
    void fractionalPageSizeReturns400AskingForAWholeNumber() throws Exception {
        mockMvc.perform(get(SEARCH).param("pageSize", "1.5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("pageSize must be a whole number, but was '1.5'"));
    }

    @Test
    void pageBeyondLastReturns400WithErrorBody() throws Exception {
        mockMvc.perform(get(SEARCH).param("page", "9999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("is past the last page")));
    }

    @Test
    void keywordWithNoMatchesReturns200WithEmptyPage() throws Exception {
        mockMvc.perform(get(SEARCH).param("keyword", "zzz-no-such-word"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", empty()))
                .andExpect(jsonPath("$.totalItems").value(0));
    }
}
