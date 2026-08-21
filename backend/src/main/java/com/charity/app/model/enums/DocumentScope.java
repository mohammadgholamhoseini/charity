package com.charity.app.model.enums;

/**
 * Which of the two document lists a {@link com.charity.app.model.DocumentCategory} belongs to.
 *
 * <p>The two lists are column-identical, so they share one table and one entity with this as the
 * discriminator rather than being duplicated. A request document filed under a {@code CENTER}
 * category -- or the reverse -- is refused in {@code DocumentService}, which is where the scope
 * assertion lives.
 */
public enum DocumentScope {

    /** Paperwork attached to a single help request. */
    REQUEST("مدارک درخواست"),

    /** Paperwork belonging to the centre itself: licence, articles of association, accounts. */
    CENTER("مدارک مرکز");

    private final String label;

    DocumentScope(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
