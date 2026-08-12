package com.charity.app.payload;

import com.charity.app.model.enums.NoticePlacement;

import java.time.OffsetDateTime;

/**
 * An announcement.
 *
 * @param expired derived from {@code endAt}, never stored -- it is what renders «منقضی» in the
 *                admin table, and deriving it means an announcement lapses without a scheduled job
 */
public record NoticeResponse(Long id,
                             String title,
                             String content,
                             NoticePlacement placement,
                             String placementLabel,
                             OffsetDateTime startAt,
                             OffsetDateTime endAt,
                             String linkUrl,
                             boolean active,
                             boolean expired,
                             OffsetDateTime createdAt,
                             OffsetDateTime updatedAt) {
}
