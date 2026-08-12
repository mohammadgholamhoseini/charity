package com.charity.app.common.error;

/**
 * The caller is authenticated but not allowed to touch this resource. Maps to 403.
 *
 * <p>Previously this and {@link ConflictException} were both signalled with IllegalStateException,
 * so "this request is not yours" and "this request is already completed" produced the same status.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
