package com.charity.app.payload;

import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.Urgency;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * A request as it appears on a card in a listing.
 *
 * <p>Request cards carry no image by design, so nothing here is about media. Timestamps are
 * ISO-8601 with a real offset rather than the old {@code "yyyy-MM-dd HH:mm"} string, so they can be
 * dropped straight into {@code <time datetime>} and JSON-LD.
 *
 * <p>There is no city field: the location a card shows is the centre's, and it arrives on
 * {@link CenterRef}.
 *
 * <p>{@code lockedByAdmin} tells the centre panel that this one was taken down by an admin and it
 * cannot put it back, so the dialog can say so rather than offering an action that 403s. It is not
 * sensitive -- a deactivated request never appears in a public listing at all.
 *
 * <p>{@code announced} is true once every enabled messaging channel carries this request, and is
 * what decides whether the panel offers the «انتشار در کانال» button. Same reasoning about
 * sensitivity: it says only whether we posted to our own public channel. With no channel
 * configured it is vacuously true, so the button never appears on an installation that has no
 * bots -- which is the wanted behaviour rather than an accident.
 */
public record RequestSummary(Long id,
                             String code,
                             String slug,
                             String title,
                             String summary,
                             BigDecimal amountNeeded,
                             RequestStatus status,
                             String statusLabel,
                             boolean lockedByAdmin,
                             boolean announced,
                             Urgency urgency,
                             String urgencyLabel,
                             CategoryRef category,
                             CenterRef center,
                             OffsetDateTime createdAt,
                             OffsetDateTime publishedAt,
                             OffsetDateTime updatedAt) {
}
