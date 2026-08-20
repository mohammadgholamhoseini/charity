package com.charity.app.service;

import com.charity.app.common.error.IncorrectCurrentPasswordException;
import com.charity.app.common.error.ValidationException;
import com.charity.app.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * The half the two self-service password-change paths share -- {@code PUT /api/center/me} and
 * {@code PUT /api/admin/me}. They are the same operation wearing two forms, and every time one of
 * them was fixed alone the other kept the bug, so the checks live here once.
 */
@Component
@RequiredArgsConstructor
class PasswordChangeGuard {

    private final PasswordEncoder passwordEncoder;

    /**
     * A present-but-blank new password is a rejection, not a no-op. {@code @Size(min = 8)} happily
     * passes eight spaces, so ignoring it answered 200 «profile saved» while the hash was untouched
     * and the account could no longer be signed into with what was typed. The admin reset path has
     * refused this all along via {@code @NotBlank}; these two now agree with it.
     */
    void requireUsableNewPassword(String newPassword) {
        if (newPassword != null && newPassword.isBlank()) {
            throw new ValidationException("رمز عبور جدید نمی‌تواند خالی باشد");
        }
    }

    /**
     * Call before mutating anything the caller sent: a wrong current password aborts the whole save,
     * and the message only mentions the password, so nothing may be left half-applied for the caller
     * to discover later.
     *
     * <p>A miss is counted against the same failed-attempt counter and the same temporary lock as a
     * failed sign-in -- the login screen was throttled and this path was not, which made a stolen
     * token brute-forceable at full speed. The count itself is <b>not</b> made from here: both
     * callers are mid-transaction and holding a pooled connection, and asking for a second one from
     * inside that is what deadlocked the pool. This throws instead, naming the account, and
     * {@code GlobalExceptionHandler} counts the attempt after the transaction has unwound. See
     * {@link IncorrectCurrentPasswordException}.
     *
     * @param user the caller's own account, resolved from the security context by the caller; the
     *     username counted is taken from here and never from the request payload
     */
    void verifyCurrentPassword(User user, String currentPassword) {
        if (currentPassword != null && passwordEncoder.matches(currentPassword, user.getPassword())) {
            return;
        }
        throw new IncorrectCurrentPasswordException(user.getUsername());
    }

    /**
     * Sets the hash and clears any standing lock -- a password that has just been proven and changed
     * must not stay unusable for the rest of the lock window. The caller's transaction persists it.
     */
    void applyNewPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
    }
}
