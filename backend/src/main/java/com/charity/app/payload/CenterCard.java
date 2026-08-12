package com.charity.app.payload;

import java.util.List;

/** A centre as it appears in the public centres listing. No account or banking fields. */
public record CenterCard(Long id,
                         String name,
                         String slug,
                         String logoUrl,
                         String description,
                         CityRef city,
                         List<CategoryRef> categories,
                         long activeRequestCount) {
}
