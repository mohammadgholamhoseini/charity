package com.charity.app.payload;

import com.charity.app.model.enums.UserRole;

import java.time.OffsetDateTime;

/** Replaces the raw LinkedHashMap the user service used to return. */
public record UserProfileResponse(Long id,
                                  String username,
                                  String email,
                                  String fullName,
                                  UserRole role,
                                  OffsetDateTime lastLoginAt,
                                  OffsetDateTime createdAt) {
}
