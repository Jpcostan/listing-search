package com.listingsearch.service;

/** Thrown when a search request is invalid. The message is shown to the user as a 400 error. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
