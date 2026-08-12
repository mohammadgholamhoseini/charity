package com.charity.app.model.enums;

/**
 * Where an announcement appears on the public site.
 *
 * <p>Renamed from {@code Position{FOOTER, BANNER}} to match the redesign's wording. V4 migrated
 * {@code BANNER} to {@code TOP_BANNER}.
 */
public enum NoticePlacement {

    /** The dismissible dark strip above the header. */
    TOP_BANNER("بنر بالای صفحه"),

    /** A single line in the footer. */
    FOOTER("پاورقی");

    private final String label;

    NoticePlacement(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
