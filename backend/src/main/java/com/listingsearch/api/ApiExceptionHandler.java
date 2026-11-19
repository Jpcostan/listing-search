package com.listingsearch.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.listingsearch.service.ValidationException;

/**
 * Turns bad-request exceptions into a JSON body: {"error": "..."} with HTTP 400.
 * Anything else falls through to Spring Boot's default 500 response, which is also JSON
 * with an "error" field and never includes a stack trace.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** A rule in SearchValidator (or the page-past-the-end check) failed. */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(ValidationException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    /** A parameter was the wrong type, e.g. minPrice=abc. Message names the parameter. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // page, pageSize and minBedrooms are Integers, so "1.5" is a number but still not acceptable.
        boolean needsWholeNumber = Integer.class.equals(ex.getRequiredType());
        String expected = needsWholeNumber ? "a whole number" : "a number";
        return new ErrorResponse(ex.getName() + " must be " + expected + ", but was '" + ex.getValue() + "'");
    }
}
