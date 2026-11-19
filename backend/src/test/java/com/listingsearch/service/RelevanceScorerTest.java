package com.listingsearch.service;

import static com.listingsearch.TestListings.listing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.listingsearch.model.Listing;

class RelevanceScorerTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-11-19");
    private static final double TOLERANCE = 1e-9;

    private final RelevanceScorer scorer = new RelevanceScorer();

    @Test
    void priceExactlyOnBudgetGivesBudgetFitOfOne() {
        assertThat(scorer.budgetFit(500_000, 500_000)).isEqualTo(1.0);
    }

    @Test
    void priceAtDoubleTheBudgetGivesBudgetFitOfZero() {
        assertThat(scorer.budgetFit(1_000_000, 500_000)).isEqualTo(0.0);
    }

    @Test
    void budgetFitIsSymmetricAboveAndBelowBudget() {
        double tenPercentOver = scorer.budgetFit(550_000, 500_000);
        double tenPercentUnder = scorer.budgetFit(450_000, 500_000);

        assertThat(tenPercentOver).isCloseTo(0.9, within(TOLERANCE));
        assertThat(tenPercentUnder).isCloseTo(0.9, within(TOLERANCE));
    }

    @Test
    void budgetFitNeverGoesBelowZero() {
        assertThat(scorer.budgetFit(5_000_000, 500_000)).isEqualTo(0.0);
    }

    @Test
    void listedTodayGivesRecencyOfOne() {
        assertThat(scorer.recency(TODAY, TODAY)).isEqualTo(1.0);
    }

    @Test
    void recencyFallsLinearlyAcrossTheWindow() {
        assertThat(scorer.recency(TODAY.minusDays(45), TODAY)).isCloseTo(0.5, within(TOLERANCE));
    }

    @Test
    void listedAtOrBeyondTheWindowGivesRecencyOfZero() {
        assertThat(scorer.recency(TODAY.minusDays(90), TODAY)).isEqualTo(0.0);
        assertThat(scorer.recency(TODAY.minusDays(400), TODAY)).isEqualTo(0.0);
    }

    @Test
    void futureListedDateCountsAsListedToday() {
        assertThat(scorer.recency(TODAY.plusDays(5), TODAY)).isEqualTo(1.0);
    }

    @Test
    void scoreCombinesBudgetFitAndRecencyWithWeights() {
        // budgetFit = 1 - 50,000/500,000 = 0.9; recency = 1 - 45/90 = 0.5
        // score = 0.7 * 0.9 + 0.3 * 0.5 = 0.78
        Listing listing = listing("A1", "MLS_A", "Fairfax", 450_000, 2, "2026-10-05", "");

        assertThat(scorer.score(listing, 500_000.0, TODAY)).isEqualTo(0.78);
    }

    @Test
    void scoreWithoutTargetBudgetIsRecencyOnly() {
        Listing listing = listing("A1", "MLS_A", "Fairfax", 450_000, 2, "2026-10-05", "");

        assertThat(scorer.score(listing, null, TODAY)).isEqualTo(0.5);
    }

    @Test
    void scoreIsRoundedToThreeDecimals() {
        // recency = 1 - 1/90 = 0.98888...
        Listing listing = listing("A1", "MLS_A", "Fairfax", 450_000, 2, "2026-11-18", "");

        assertThat(scorer.score(listing, null, TODAY)).isEqualTo(0.989);
    }
}
