package com.charity.app.common;

import com.charity.app.common.error.ValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Map;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * Builds a {@link Pageable} from untrusted query parameters.
 *
 * <p>Two things this exists to prevent. {@code size} used to be unbounded, so {@code ?size=100000}
 * would happily try to materialise the whole table. And binding Spring's own {@code Pageable}
 * resolver would let a caller sort by any entity property, which both leaks the schema and turns a
 * typo into a 500 from {@code PropertyReferenceException}; an allowlist gives a clean 400 instead.
 */
public final class Paging {

    public static final int MAX_SIZE = 60;
    public static final int DEFAULT_SIZE = 12;

    /** Public-facing sort keys. Anything not in here is rejected. */
    private static final Map<String, Sort> SORTS = Map.of(
            "urgent", Sort.by(DESC, "urgencyRank").and(Sort.by(DESC, "createdAt")),
            "newest", Sort.by(DESC, "createdAt"),
            "oldest", Sort.by(ASC, "createdAt"),
            "deadline", Sort.by(Sort.Order.asc("deadline").nullsLast()).and(Sort.by(DESC, "createdAt")),
            "amount_desc", Sort.by(DESC, "amountNeeded"),
            "amount_asc", Sort.by(ASC, "amountNeeded"));

    public static final String DEFAULT_SORT = "urgent";

    private Paging() {
    }

    public static Pageable of(Integer page, Integer size, String sort) {
        return PageRequest.of(safePage(page), safeSize(size), resolveSort(sort));
    }

    /** For listings that have their own fixed ordering. */
    public static Pageable of(Integer page, Integer size, Sort sort) {
        return PageRequest.of(safePage(page), safeSize(size), sort);
    }

    public static Sort resolveSort(String sort) {
        String key = (sort == null || sort.isBlank()) ? DEFAULT_SORT : sort.trim().toLowerCase(Locale.ROOT);
        Sort resolved = SORTS.get(key);
        if (resolved == null) {
            throw new ValidationException("پارامتر مرتب‌سازی نامعتبر است");
        }
        return resolved;
    }

    private static int safePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private static int safeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
