package com.charity.app.payload;

import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.Urgency;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * @param statusNote   why it was rejected or deactivated; only populated for the owning centre and
 *                     admins, never for the public
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
                                    LocalDate deadline,
                                    RequestStatus status,
                                    String statusLabel,
                                    boolean isActive,
                                    String statusNote,
                                    Urgency urgency,
                                    String urgencyLabel,
                                    CategoryRef category,
                                    CenterRef center,
                                    CityRef city,
                                    String imageUrl,
                                    String contactInfo,
                                    List<String> documents,
                                    Map<String, Object> details,
                                    String metaTitle,
                                    String metaDescription,
                                    OffsetDateTime createdAt,
                                    OffsetDateTime publishedAt,
                                    OffsetDateTime updatedAt) {
}
