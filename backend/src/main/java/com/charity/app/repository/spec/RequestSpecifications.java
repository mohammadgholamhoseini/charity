package com.charity.app.repository.spec;

import com.charity.app.common.SlugUtil;
import com.charity.app.model.Request;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.Urgency;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;

/**
 * Composable filters for the request listings.
 *
 * <p>These replace an if/else ladder that silently dropped filters: passing a centre discarded the
 * search term and every other facet, and a category could not be combined with a search at all.
 * Every predicate here returns null when it has nothing to say, so
 * {@code Specification.allOf(...)} simply skips it and any combination works.
 *
 * <p>Note the explicit {@link JoinType#LEFT} joins. A traversal like {@code root.get("city").get("province")}
 * produces an INNER join, which would silently hide every request whose city is null -- and city is
 * nullable, because it was backfilled from centres that do not all have one.
 */
public final class RequestSpecifications {

    private RequestSpecifications() {
    }

    /** Soft-deleted rows are excluded everywhere except the explicit "is this a 410?" lookup. */
    public static Specification<Request> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Request> statusIn(Collection<RequestStatus> statuses) {
        return (root, query, cb) -> isEmpty(statuses) ? null : root.get("status").in(statuses);
    }

    public static Specification<Request> categoryIdIn(Collection<Long> categoryIds) {
        return (root, query, cb) -> isEmpty(categoryIds) ? null : root.get("category").get("id").in(categoryIds);
    }

    public static Specification<Request> categorySlugIn(Collection<String> slugs) {
        return (root, query, cb) -> isEmpty(slugs)
                ? null
                : root.join("category", JoinType.LEFT).get("slug").in(slugs);
    }

    public static Specification<Request> urgencyIn(Collection<Urgency> urgencies) {
        return (root, query, cb) -> isEmpty(urgencies) ? null : root.get("urgency").in(urgencies);
    }

    public static Specification<Request> cityIdIn(Collection<Long> cityIds) {
        return (root, query, cb) -> isEmpty(cityIds)
                ? null
                : root.join("city", JoinType.LEFT).get("id").in(cityIds);
    }

    public static Specification<Request> cityNameIn(Collection<String> names) {
        return (root, query, cb) -> isEmpty(names)
                ? null
                : root.join("city", JoinType.LEFT).get("name").in(names);
    }

    public static Specification<Request> provinceIdEquals(Long provinceId) {
        return (root, query, cb) -> provinceId == null
                ? null
                : cb.equal(root.join("city", JoinType.LEFT).join("province", JoinType.LEFT).get("id"), provinceId);
    }

    public static Specification<Request> centerIdEquals(Long centerId) {
        return (root, query, cb) -> centerId == null ? null : cb.equal(root.get("center").get("id"), centerId);
    }

    public static Specification<Request> centerSlugEquals(String slug) {
        return (root, query, cb) -> isBlank(slug)
                ? null
                : cb.equal(root.join("center", JoinType.LEFT).get("slug"), slug);
    }

    /**
     * Free-text search over title and description, plus an exact match on the public code so a
     * visitor can paste «۱۰۲۴» straight from a printed notice. The term is folded through the same
     * Persian normalisation the slugs use, otherwise typing on an Arabic keyboard layout returns
     * nothing.
     */
    public static Specification<Request> textMatches(String term) {
        return (root, query, cb) -> {
            if (isBlank(term)) {
                return null;
            }
            String normalized = SlugUtil.normalizePersian(term.trim()).toLowerCase(Locale.ROOT);
            String like = "%" + normalized + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like),
                    cb.equal(cb.lower(root.get("code")), normalized));
        };
    }

    /** Hides requests whose deadline has passed. */
    public static Specification<Request> deadlineNotPassed(LocalDate today) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("deadline")),
                cb.greaterThanOrEqualTo(root.get("deadline"), today));
    }

    private static boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
