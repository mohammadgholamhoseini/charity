package com.charity.app.model.enums;

/**
 * How urgent a request is.
 *
 * <p>The numeric {@link #rank()} is persisted alongside the enum in {@code requests.urgency_rank}
 * so ordering is a plain column sort. It used to be an identical {@code CASE WHEN} repeated across
 * six JPQL queries, which meant adding a level required editing all six.
 */
public enum Urgency {

    LOW(0, "کم"),
    MEDIUM(1, "متوسط"),
    HIGH(2, "بالا"),
    URGENT(3, "فوری");

    private final int rank;
    private final String label;

    Urgency(int rank, String label) {
        this.rank = rank;
        this.label = label;
    }

    public int rank() {
        return rank;
    }

    public String label() {
        return label;
    }
}
