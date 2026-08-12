package com.charity.app.common.error;

/** A parameter was syntactically fine but semantically invalid. Maps to 400. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
