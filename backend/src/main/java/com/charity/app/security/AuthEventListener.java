package com.charity.app.security;

import com.charity.app.model.User;
import com.charity.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Maintains the failed-attempt counters behind the login screen's promise that «پس از ۵ تلاش
 * ناموفق حساب موقتاً قفل می‌شود» -- which nothing previously enforced.
 *
 * <p>Driven by Spring Security's own authentication events, so it needs no changes in the auth
 * service and cannot be bypassed by a different entry point.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventListener {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository users;

    @EventListener
    @Transactional
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String username = String.valueOf(event.getAuthentication().getName());
        users.findByUsername(username).ifPresent(user -> {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= MAX_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plus(LOCK_DURATION));
                log.warn("Locked account '{}' after {} failed attempts", username, attempts);
            }
            users.save(user);
        });
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        users.findByUsername(event.getAuthentication().getName()).ifPresent(user -> {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            user.setLastLoginAt(LocalDateTime.now());
            users.save(user);
        });
    }
}
