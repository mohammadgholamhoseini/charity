package com.charity.app.common.error;

import lombok.Getter;

/** The request is well-formed but conflicts with current state. Maps to 409. */
@Getter
public class ConflictException extends RuntimeException {

    /** Machine-readable discriminator so the UI can react, e.g. CATEGORY_IN_USE. */
    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }
}
