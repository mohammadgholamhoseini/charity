package com.charity.app.payload;

import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.Urgency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * A request as it appears on a card in a listing.
 *
 * <p>Request cards carry no image by design, so nothing here is about media. Timestamps are
 * ISO-8601 with a real offset rather than the old {@code "yyyy-MM-dd HH:mm"} string, so they can be
 * dropped straight into {@code <time datetime>} and JSON-LD.
 */
public record RequestSummary(Long id,
                             String code,
                             String slug,
                             String title,
                             String summary,
                             BigDecimal amountNeeded,
                             LocalDate deadline,
                             RequestStatus status,
                             String statusLabel,
                             Urgency urgency,
                             String urgencyLabel,
                             CategoryRef category,
                             CenterRef center,
                             CityRef city,
                             OffsetDateTime createdAt,
                             OffsetDateTime publishedAt,
                             OffsetDateTime updatedAt) {
}
