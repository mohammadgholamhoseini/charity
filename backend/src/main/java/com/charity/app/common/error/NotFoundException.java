package com.charity.app.common.error;

/** The addressed resource does not exist. Maps to 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
