package com.charity.app.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * The single error shape every failing endpoint returns.
 *
 * <p>Being a record rather than a {@code Map.of(...)} is the point: {@code Map.of} rejects null
 * values, so the previous handler threw a {@link NullPointerException} of its own whenever it caught
 * an exception whose {@code getMessage()} was null -- which is common.
 *
 * @param message  human-readable, in Persian, safe to show a user
 * @param code     optional machine-readable discriminator
 * @param fields   per-field validation messages, keyed by field name
 * @param traceId  correlates a 500 with the server log; the log holds the detail, the client does not
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String message,
                       String code,
                       Instant timestamp,
                       Map<String, String> fields,
                       String traceId) {

    public static ApiError of(String message) {
        return new ApiError(message, null, Instant.now(), null, null);
    }

    public static ApiError of(String message, String code) {
        return new ApiError(message, code, Instant.now(), null, null);
    }

    public static ApiError fields(String message, Map<String, String> fields) {
        return new ApiError(message, "VALIDATION_FAILED", Instant.now(), fields, null);
    }

    public static ApiError internal(String traceId) {
        return new ApiError("خطای داخلی سرور رخ داد.", "INTERNAL_ERROR", Instant.now(), null, traceId);
    }
}
