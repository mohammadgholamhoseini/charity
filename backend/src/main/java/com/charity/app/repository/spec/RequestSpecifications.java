package com.charity.app.repository.spec;

import com.charity.app.common.SlugUtil;
import com.charity.app.model.Request;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.Urgency;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

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
 * <p>Note the explicit {@link JoinType#LEFT} joins. A traversal like
 * {@code root.get("center").get("city")} produces an INNER join, which would silently hide every
 * request whose centre has no city recorded -- and a centre's city is optional.
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

    /*
     * The three location facets reach the city through the centre.
     *
     * A request has no city of its own. It used to, but that column was populated from
     * centers.city_id when it was introduced and nothing ever set it independently, so all the
     * second copy achieved was a way for a request and its centre to claim different cities.
     * Filtering through the join keeps every existing URL -- ?city=مشهد, ?province=8 -- answering
     * the same rows it did before.
     */

    public static Specification<Request> cityIdIn(Collection<Long> cityIds) {
        return (root, query, cb) -> isEmpty(cityIds)
                ? null
                : centerCity(root).get("id").in(cityIds);
    }

    public static Specification<Request> cityNameIn(Collection<String> names) {
        return (root, query, cb) -> isEmpty(names)
                ? null
                : centerCity(root).get("name").in(names);
    }

    public static Specification<Request> provinceIdEquals(Long provinceId) {
        return (root, query, cb) -> provinceId == null
                ? null
                : cb.equal(centerCity(root).join("province", JoinType.LEFT).get("id"), provinceId);
    }

    /** Left joins so a centre with no city recorded does not vanish from unfiltered listings. */
    private static Join<?, ?> centerCity(Root<Request> root) {
        return root.join("center", JoinType.LEFT).join("city", JoinType.LEFT);
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

    private static boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
