package com.charity.app.security;

import com.charity.app.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Feeds Spring Security's own authentication events into {@link LoginAttemptService}, so the login
 * screen needs no changes in the auth service and no sign-in path can slip past the counter.
 *
 * <p>It is only the adapter, not the counter. The counting, the threshold and the lock live in
 * {@link LoginAttemptService} because the login screen is not the only way to get a password wrong:
 * the self-service password change verifies {@code currentPassword} itself and publishes no event,
 * so {@code GlobalExceptionHandler} counts that one. Adding the logic back in here would mean the
 * two paths counting against different rules again -- which is how one of them ended up
 * unthrottled.
 *
 * <p>Nothing here is {@code @Transactional}, and nothing here may become so. {@link
 * LoginAttemptService#recordFailure} must be entered with no transaction open -- see that class --
 * and this listener is one of the two places that guarantees it: Spring Security publishes these
 * events after {@code CustomUserDetailsService}'s read-only transaction has already closed.
 */
@Component
@RequiredArgsConstructor
public class AuthEventListener {

    private final LoginAttemptService attempts;

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        attempts.recordFailure(String.valueOf(event.getAuthentication().getName()));
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        attempts.recordSuccess(String.valueOf(event.getAuthentication().getName()));
    }
}
