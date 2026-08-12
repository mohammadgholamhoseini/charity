package com.charity.app.common.error;

/**
 * The resource existed and has been withdrawn. Maps to 410.
 *
 * <p>Worth distinguishing from 404: search engines drop 410s from the index considerably faster,
 * which is why deletion is soft rather than a real DELETE.
 */
public class GoneException extends RuntimeException {
    public GoneException(String message) {
        super(message);
    }
}
