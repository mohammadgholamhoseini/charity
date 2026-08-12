package com.charity.app.mapper;

import com.charity.app.common.AppUrls;
import com.charity.app.model.User;
import com.charity.app.payload.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Replaces the hand-built LinkedHashMap the user service used to return. */
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final AppUrls urls;

    public UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                urls.iso(user.getLastLoginAt()),
                urls.iso(user.getCreatedAt()));
    }
}
