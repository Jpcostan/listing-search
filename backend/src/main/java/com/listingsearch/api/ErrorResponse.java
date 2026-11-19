package com.listingsearch.api;

/** The JSON body of every error: {"error": "..."}. */
public record ErrorResponse(String error) {
}
