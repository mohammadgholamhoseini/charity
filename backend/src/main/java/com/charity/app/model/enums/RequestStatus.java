package com.charity.app.model.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a help request.
 *
 * <p>Extracted from the entity so it no longer clashes with {@link CenterStatus} and can be
 * referenced from specifications, DTOs and the status policy without qualification.
 *
 * <p>{@code DRAFT} is new: the centre panel offers «ذخیره پیش‌نویس» alongside «ارسال برای بررسی»,
 * which is a genuinely different state from «در انتظار انتشار». No migration was needed because the
 * column stores enum names as text.
 */
public enum RequestStatus {

    /** Saved by the centre but never submitted. Visible only to its own centre. */
    DRAFT("پیش‌نویس"),

    /** Submitted and awaiting an admin decision. The default for anything newly submitted. */
    PENDING("در انتظار انتشار"),

    /** Live on the public site. */
    PUBLISHED("منتشرشده"),

    /** Turned down by an admin, who must record a reason. */
    REJECTED("رد شده"),

    /** Help was received. Drops out of the active list but keeps its URL. */
    COMPLETED("تکمیل‌شده"),

    /** Withdrawn from the listing without being deleted. */
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
