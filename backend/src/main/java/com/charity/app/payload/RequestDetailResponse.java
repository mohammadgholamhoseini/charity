package com.charity.app.payload;

import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.Urgency;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Everything the request detail page needs, including its own SEO metadata, so the server-rendered
 * page can build its title, canonical URL and JSON-LD without a second round trip.
 *
 * @param canonicalUrl absolute public URL; the client compares it against the URL that was requested
 *                     and issues a 301 when a slug has since changed
 * @param isActive     false once COMPLETED -- the page stays up and keeps its URL, but renders as
 *                     «تأمین شد» and drops out of the active listing
 * @param statusNote   why it was deactivated; only populated for the owning centre and admins,
 *                     never for the public
 * @param lockedByAdmin an admin's takedown is in force, so the owning centre cannot restore this
 *                      one itself -- the panel uses it to explain rather than offer a 403
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RequestDetailResponse(Long id,
                                    String code,
                                    String slug,
                                    String canonicalUrl,
                                    String title,
                                    String summary,
                                    String description,
                                    BigDecimal amountNeeded,
                                    String amountCurrency,
                                    RequestStatus status,
                                    String statusLabel,
                                    boolean isActive,
                                    boolean lockedByAdmin,
                                    String statusNote,
                                    Urgency urgency,
                                    String urgencyLabel,
                                    CategoryRef category,
                                    CenterRef center,
                                    String imageUrl,
                                    List<String> documents,
                                    Map<String, Object> details,
                                    String metaTitle,
                                    String metaDescription,
                                    OffsetDateTime createdAt,
                                    OffsetDateTime publishedAt,
                                    OffsetDateTime updatedAt) {
}
