package com.listingsearch.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import com.listingsearch.model.Listing;

/**
 * The relevance formula:
 *   score = 0.7 * budgetFit + 0.3 * recency, rounded to 3 decimals.
 * With no targetBudget, score = recency.
 */
@Component
public class RelevanceScorer {

    static final double BUDGET_WEIGHT = 0.7;
    static final double RECENCY_WEIGHT = 0.3;
    static final int RECENCY_WINDOW_DAYS = 90;

    /** Combines budgetFit and recency into one score. targetBudget may be null. */
    public double score(Listing listing, Double targetBudget, LocalDate today) {
        double recency = recency(listing.listedDate(), today);

        double score;
        if (targetBudget == null) {
            score = recency;
        } else {
            score = BUDGET_WEIGHT * budgetFit(listing.price(), targetBudget) + RECENCY_WEIGHT * recency;
        }
        return Math.round(score * 1000) / 1000.0;
    }

    /** 1 when price equals the budget, falling linearly to 0 at double the budget (or at free). */
    double budgetFit(double price, double targetBudget) {
        double distanceFromBudget = Math.abs(price - targetBudget);
        return Math.max(0, 1 - distanceFromBudget / targetBudget);
    }

    /** 1 when listed today, falling linearly to 0 at RECENCY_WINDOW_DAYS old. */
    double recency(LocalDate listedDate, LocalDate today) {
        long daysSinceListed = ChronoUnit.DAYS.between(listedDate, today);
        if (daysSinceListed < 0) {
            daysSinceListed = 0; // a future-dated listing counts as listed today
        }
        return Math.max(0, 1 - (double) daysSinceListed / RECENCY_WINDOW_DAYS);
    }
}
