package com.charity.app.common.error;

import lombok.Getter;

/**
 * The {@code currentPassword} on a self-service password change did not match. Maps to 400, like any
 * other {@link ValidationException} -- and additionally counts one failed attempt against the login
 * throttle, which is why it is its own type.
 *
 * <p><b>The counting is deliberately not done where this is thrown.</b> Both throw sites sit inside
 * the {@code @Transactional} profile-save that owns a pooled connection for the whole call. Asking
 * {@code LoginAttemptService} to count from in there meant a second connection was requested while
 * the first was still held; at {@code active == maximum-pool-size} every holder then waited 30s for
 * a connection only another holder could release, and 13 of 20 concurrent wrong-password attempts
 * came back {@code 500} while all other database work stalled behind them. Anyone with one valid
 * token could trigger it -- the same attacker the throttle exists to stop.
 *
 * <p>So the throw carries the username instead, and
 * {@code GlobalExceptionHandler.handleIncorrectCurrentPassword} does the counting once the caller's
 * transaction has unwound and given its connection back. Nothing nests, and the rollback that
 * discards the profile edits submitted alongside the wrong password can no longer discard the
 * attempt with them -- there is no longer a caller transaction for the counter to share.
 *
 * <p>The two messages live here together because the handler picks between them by the result of the
 * count, and a wording that drifts apart in two files is a wording that drifts apart.
 */
@Getter
public class IncorrectCurrentPasswordException extends ValidationException {

    /** What the caller is told when the account is still below the threshold. */
    public static final String MESSAGE = "رمز عبور فعلی نادرست است";

    /** ...and on the attempt that trips the lock. The panel renders both. */
    public static final String LOCKED_MESSAGE = "رمز عبور فعلی نادرست است. حساب شما به دلیل تلاش‌های ناموفق پیاپی موقتاً قفل شد.";

    /**
     * The account to count the attempt against. Always the authenticated caller's own username,
     * resolved from the security context -- never a name taken from the request body, which would
     * hand any signed-in user a way to lock any other account out.
     */
    private final String username;

    public IncorrectCurrentPasswordException(String username) {
        super(MESSAGE);
        this.username = username;
    }
}
