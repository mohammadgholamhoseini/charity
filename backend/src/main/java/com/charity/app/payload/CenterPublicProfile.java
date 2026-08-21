package com.charity.app.payload;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A centre's public page.
 *
 * <p>Banking details are deliberately public: there is no online payment, so a visitor who wants to
 * help contacts the centre and pays it directly. Account fields (username, email) are not.
 *
 * <p>{@code documents} is the centre's own paperwork -- licence, articles, accounts -- and is
 * public by decision. {@link CenterCard} and {@link CenterRef} carry none of it: the listing would
 * pay an N+1 for something no card renders.
 */
public record CenterPublicProfile(Long id,
                                  String name,
                                  String slug,
                                  String canonicalUrl,
                                  String fullName,
                                  String description,
                                  String contactPhone,
                                  String responseHours,
                                  String address,
                                  String cardNumber,
                                  String sheba,
                                  String logoUrl,
                                  CityRef city,
                                  List<CategoryRef> categories,
                                  List<CenterDocumentResponse> documents,
                                  long activeRequestCount,
                                  OffsetDateTime updatedAt) {
}
