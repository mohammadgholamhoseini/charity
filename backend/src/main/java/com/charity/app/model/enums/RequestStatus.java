package com.charity.app.model.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a help request.
 *
 * <p>Extracted from the entity so it no longer clashes with {@link CenterStatus} and can be
 * referenced from specifications, DTOs and the status policy without qualification.
 *
 * <p>A centre publishes its own requests; there is no admin queue in between. {@code PENDING} and
 * {@code REJECTED} are therefore no longer reachable — see {@code RequestStatusPolicy} for the
 * workflow that remains. They are kept as constants rather than deleted because the column stores
 * enum names as text, and an enum that throws on a row written by an older version of the app is a
 * worse outcome than two values nothing produces.
 */
public enum RequestStatus {

    /** Saved by the centre but not published. Visible only to its own centre. */
    DRAFT("پیش‌نویس"),

    /** @deprecated no longer reachable; kept so pre-V9 rows still deserialise. */
    @Deprecated
    PENDING("در انتظار انتشار"),

    /** Live on the public site. Where a request lands the moment its centre publishes it. */
    PUBLISHED("منتشرشده"),

    /** @deprecated no longer reachable; kept so pre-V9 rows still deserialise. */
    @Deprecated
    REJECTED("رد شده"),

    /** Help was received. Drops out of the active list but keeps its URL. */
    COMPLETED("تکمیل‌شده"),

    /** Taken down by an admin, with a recorded reason. Not deleted, so it can go back up. */
    INACTIVE("غیرفعال");

    private final String label;

    RequestStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * Statuses a visitor may ask for. {@code COMPLETED} is requestable but not included by default,
     * so the listing shows only active requests while completed ones keep working URLs.
     */
    public static final Set<RequestStatus> PUBLICLY_REQUESTABLE = EnumSet.of(PUBLISHED, COMPLETED);

    /** What the public list shows when no status filter is supplied. */
    public static final Set<RequestStatus> PUBLIC_DEFAULT = EnumSet.of(PUBLISHED);

    public boolean isPubliclyVisible() {
        return PUBLICLY_REQUESTABLE.contains(this);
    }
}
