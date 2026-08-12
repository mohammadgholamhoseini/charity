package com.charity.app.payload;

import com.charity.app.model.enums.UserRole;

public record AuthResponse(String token,
                           String username,
                           UserRole role,
                           Long userId,
                           String fullName,
                           Long centerId) {
}
