package com.charity.app.payload;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A centre's public page.
 *
 * <p>Banking details are deliberately public: there is no online payment, so a visitor who wants to
 * help contacts the centre and pays it directly. Account fields (username, email) are not.
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
                                  long activeRequestCount,
                                  OffsetDateTime updatedAt) {
}
