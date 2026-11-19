package com.listingsearch.service;

import static com.listingsearch.TestListings.listing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.listingsearch.model.SearchCriteria;
import com.listingsearch.repository.ListingRepository;

class SearchValidatorTest {

    private final ListingRepository repository = new ListingRepository(List.of(
            listing("A1", "MLS_A", "Fairfax", 400_000, 2, "2026-11-01", ""),
            listing("A2", "MLS_A", "Reston", 600_000, 4, "2026-11-01", "")));

    private final SearchValidator validator = new SearchValidator(repository);

    @Test
    void noParametersGivesDefaultPageAndPageSize() {
        SearchCriteria criteria = validator.validate(null, null, null, null, null, null, null, null, null);

        assertThat(criteria.page()).isEqualTo(1);
        assertThat(criteria.pageSize()).isEqualTo(10);
        assertThat(criteria.minPrice()).isNull();
        assertThat(criteria.city()).isNull();
    }

    @Test
    void validValuesArePassedThrough() {
        SearchCriteria criteria = validator.validate(100.0, 200.0, 3, "Reston", "pool", "sold", 150.0, 2, 5);

        assertThat(criteria).isEqualTo(new SearchCriteria(100.0, 200.0, 3, "Reston", "pool", "sold", 150.0, 2, 5));
    }

    @Test
    void minPriceGreaterThanMaxPriceIsRejected() {
        assertThatThrownBy(() -> validator.validate(500.0, 400.0, null, null, null, null, null, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("minPrice must not be greater than maxPrice");
    }

    @Test
    void minPriceEqualToMaxPriceIsAllowed() {
        SearchCriteria criteria = validator.validate(400.0, 400.0, null, null, null, null, null, null, null);

        assertThat(criteria.minPrice()).isEqualTo(400.0);
        assertThat(criteria.maxPrice()).isEqualTo(400.0);
    }

    @Test
    void negativePriceIsRejected() {
        assertThatThrownBy(() -> validator.validate(-1.0, null, null, null, null, null, null, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("minPrice must be 0 or greater");
        assertThatThrownBy(() -> validator.validate(null, -1.0, null, null, null, null, null, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("maxPrice must be 0 or greater");
    }

    @Test
    void notANumberPriceIsRejected() {
        assertThatThrownBy(() -> validator.validate(Double.NaN, null, null, null, null, null, null, null, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void negativeBedroomsIsRejected() {
        assertThatThrownBy(() -> validator.validate(null, null, -1, null, null, null, null, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("minBedrooms must be 0 or greater");
    }

    @Test
    void zeroOrNegativeTargetBudgetIsRejected() {
        assertThatThrownBy(() -> validator.validate(null, null, null, null, null, null, 0.0, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("targetBudget must be greater than 0");
        assertThatThrownBy(() -> validator.validate(null, null, null, null, null, null, -5.0, null, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void pageSizeZeroIsRejected() {
        assertThatThrownBy(() -> validator.validate(null, null, null, null, null, null, null, null, 0))
                .isInstanceOf(ValidationException.class)
                .hasMessage("pageSize must be between 1 and 100");
    }

    @Test
    void negativePageSizeIsRejected() {
        assertThatThrownBy(() -> validator.validate(null, null, null, null, null, null, null, null, -3))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void pageSizeAboveMaxIsRejected() {
        assertThatThrownBy(() -> validator.validate(null, null, null, null, null, null, null, null, 101))
                .isInstanceOf(ValidationException.class);
        assertThat(validator.validate(null, null, null, null, null, null, null, null, 100).pageSize()).isEqualTo(100);
    }

    @Test
    void pageZeroIsRejected() {
        assertThatThrownBy(() -> validator.validate(null, null, null, null, null, null, null, 0, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("page must be 1 or greater");
    }

    @Test
    void unknownCityIsRejectedAndErrorListsKnownCities() {
        assertThatThrownBy(() -> validator.validate(null, null, null, "Atlantis", null, null, null, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Unknown city 'Atlantis'. Known cities: Fairfax, Reston");
    }

    @Test
    void cityMatchIsCaseInsensitive() {
        SearchCriteria criteria = validator.validate(null, null, null, "  fAIRfax ", null, null, null, null, null);

        assertThat(criteria.city()).isEqualTo("Fairfax");
    }

    @Test
    void blankCityKeywordAndStatusAreTreatedAsNotSupplied() {
        SearchCriteria criteria = validator.validate(null, null, null, "   ", "", " ", null, null, null);

        assertThat(criteria.city()).isNull();
        assertThat(criteria.keyword()).isNull();
        assertThat(criteria.status()).isNull();
    }

    @Test
    void unknownStatusIsRejectedAndErrorListsKnownStatuses() {
        assertThatThrownBy(() -> validator.validate(null, null, null, null, null, "haunted", null, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Unknown status 'haunted'. Known statuses: active, pending, sold");
    }

    @Test
    void statusMatchIsCaseInsensitive() {
        SearchCriteria criteria = validator.validate(null, null, null, null, null, " PENDING ", null, null, null);

        assertThat(criteria.status()).isEqualTo("pending");
    }
}
