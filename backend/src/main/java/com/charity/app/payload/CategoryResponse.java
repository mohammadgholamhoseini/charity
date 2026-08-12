package com.charity.app.payload;

import java.time.OffsetDateTime;

/**
 * A category as the site and admin table see it.
 *
 * @param activeRequestCount drives «N درخواست فعال» on the homepage category grid and the count
 *                           column in the admin table; computed in one grouped query per page
 *                           rather than one per row
 */
public record CategoryResponse(Long id,
                               String name,
                               String slug,
                               String description,
                               String labelBg,
                               String labelText,
                               int sortOrder,
                               String iconUrl,
                               boolean active,
                               long activeRequestCount,
                               OffsetDateTime updatedAt) {
}
