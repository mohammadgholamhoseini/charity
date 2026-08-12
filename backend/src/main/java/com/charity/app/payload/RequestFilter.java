package com.charity.app.payload;

import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.Urgency;

import java.util.List;

/**
 * Every facet the request listings support, in one object.
 *
 * <p>All of these combine. The previous implementation was an if/else ladder where supplying a
 * centre silently discarded the search term and every other facet, and a category could not be
 * combined with a search at all.
 *
 * <p>Categories and cities are accepted by slug/name as well as id so the frontend can build
 * shareable, indexable URLs like {@code ?category=darman&city=مشهد} instead of leaking database ids.
 */
public record RequestFilter(List<Long> categoryIds,
                            List<String> categorySlugs,
                            List<Urgency> urgencies,
                            List<Long> cityIds,
                            List<String> cityNames,
                            Long provinceId,
                            Long centerId,
                            String centerSlug,
                            List<RequestStatus> statuses,
                            String query) {

    public static RequestFilter empty() {
        return new RequestFilter(null, null, null, null, null, null, null, null, null, null);
    }
}
