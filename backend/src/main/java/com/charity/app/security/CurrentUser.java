package com.charity.app.security;

import com.charity.app.common.error.ForbiddenException;
import com.charity.app.common.error.NotFoundException;
import com.charity.app.model.Center;
import com.charity.app.model.User;
import com.charity.app.repository.CenterRepository;
import com.charity.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the caller.
 *
 * <p>The same four lines of {@code SecurityContextHolder} plumbing were repeated in five services,
 * half of them ending in a bare {@code orElseThrow()} that produced a 500 with no message.
 */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserRepository userRepository;
    private final CenterRepository centerRepository;

    public String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("برای انجام این عملیات باید وارد شوید");
        }
        return auth.getName();
    }

    public User user() {
        return userRepository.findByUsername(username())
                .orElseThrow(() -> new NotFoundException("کاربر یافت نشد"));
    }

    /** The centre owned by the caller. Only meaningful for a CENTER account. */
    public Center center() {
        return centerRepository.findByUserId(user().getId())
                .orElseThrow(() -> new NotFoundException("مرکز خیریه‌ای برای این حساب ثبت نشده است"));
    }
}
