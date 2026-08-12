package com.charity.app.model.enums;

/**
 * Whether a centre is listed.
 *
 * <p>Reduced from {@code PENDING, APPROVED, REJECTED, INACTIVE}. There is no public registration --
 * centres exist only because an admin created them, and creation already set {@code APPROVED}
 * directly -- so {@code PENDING} and {@code REJECTED} were unreachable, as was the whole
 * "pending centres" admin screen. What remains maps to the single active/inactive switch in the
 * redesigned admin form. V8 moved any stranded rows to {@code APPROVED}.
 */
public enum CenterStatus {

    APPROVED("فعال"),
    INACTIVE("غیرفعال");

    private final String label;

    CenterStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
