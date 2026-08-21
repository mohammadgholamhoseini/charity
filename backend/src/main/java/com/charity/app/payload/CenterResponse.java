package com.charity.app.payload;

import com.charity.app.model.enums.CenterStatus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The full centre record, for the centre's own profile screen and for admin screens.
 *
 * <p>The same centre used to be serialised in three unrelated shapes -- this record, a hand-built
 * LinkedHashMap, and the raw entity -- which meant fields leaked or went missing depending on which
 * endpoint you happened to call. There are now exactly three intentional shapes: this one,
 * {@link CenterCard} for public listings and {@link CenterPublicProfile} for the public detail page.
 * Account fields (username, email) appear only here.
 */
public record CenterResponse(Long id,
                             String name,
                             String slug,
                             String fullName,
                             String description,
                             String contactPhone,
                             String responseHours,
                             String username,
                             String email,
                             String address,
                             String cardNumber,
                             String sheba,
                             String logoUrl,
                             CenterStatus status,
                             String statusLabel,
                             CityRef city,
                             List<CategoryRef> categories,
                             List<CenterDocumentResponse> documents,
                             long activeRequestCount,
                             OffsetDateTime createdAt,
                             OffsetDateTime updatedAt) {
}
