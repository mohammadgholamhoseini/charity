package com.charity.app.model.enums;

/**
 * The two roles the platform has. There is no public registration: admins create centre accounts.
 */
public enum UserRole {

    ADMIN("ادمین"),
    CENTER("مرکز خیریه");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Spring Security expects authorities to carry the {@code ROLE_} prefix. */
    public String authority() {
        return "ROLE_" + name();
    }
}
