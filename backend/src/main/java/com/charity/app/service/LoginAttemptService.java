package com.charity.app.service;

import com.charity.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * The failed-attempt counter and the temporary lock behind the login screen's promise that «پس از ۵
 * تلاش ناموفق حساب موقتاً قفل می‌شود» -- which nothing enforced before V6.
 *
 * <p>It is its own bean because it has <b>two</b> call sites, not one. {@code AuthEventListener}
 * feeds it Spring Security's authentication events, and {@code GlobalExceptionHandler} counts a
 * wrong {@code currentPassword} on {@code PUT /api/center/me} or {@code PUT /api/admin/me} -- a
 * path Spring Security never sees, and one whose whole purpose is throttling, since otherwise a
 * stolen token could be brute-forced into a permanent takeover at full speed. Anything that counts
 * a failed credential check belongs here; a third counter somewhere else is how the first two
 * drifted apart.
 *
 * <p><b>One constraint binds every call site, present and future: call this with no transaction
 * open.</b> Both current callers do, and neither by luck. Spring Security publishes its failure
 * event after {@code CustomUserDetailsService}'s own read-only transaction has closed, and an
 * {@code @ExceptionHandler} runs only once the service transaction that threw has rolled back and
 * handed its connection back to the pool; {@code open-in-view} is false, so no transaction spans
 * the request either.
 *
 * <p>That constraint replaces the {@code REQUIRES_NEW} this used to carry, which was actively
 * harmful. {@code PasswordChangeGuard} called it from inside the {@code @Transactional} profile-save
 * so that the save's rollback could not discard the count -- but a nested {@code REQUIRES_NEW} asks
 * the pool for a <i>second</i> connection while the caller still holds the first, and once every
 * connection in the pool belongs to such a caller, each one waits 30s for a connection only another
 * waiter can release. Measured at 20 concurrent wrong-password attempts: 13 answered {@code 500}
 * after the Hikari timeout, and all other database work stalled behind them. Counting from the
 * exception handler removes the nesting and, with it, the reason {@code REQUIRES_NEW} existed:
 * there is no caller transaction left to be rolled back, so plain {@code REQUIRED} starts a fresh
 * one and commits on its own. It also fails safe if a future caller ignores the constraint above --
 * {@code REQUIRED} would join that caller's transaction and risk losing a count, where
 * {@code REQUIRES_NEW} would deadlock the pool.
 *
 * <p>Still call it through the injected bean rather than {@code this}: self-invocation bypasses the
 * proxy, and an {@code @Modifying} query with no transaction throws {@code TransactionRequiredException}.
 *
 * <p>Counting is done with atomic {@code UPDATE} statements rather than read-modify-write, and that
 * is the point of the class rather than an implementation detail. Read-then-save loses concurrent
 * attempts against each other: fifty simultaneous guesses each read {@code failed_attempts = 0} and
 * each write {@code 1}, the counter lands on {@code 1}, and the throttle never trips no matter how
 * fast the attacker goes -- while a sequential five-in-a-row test passes cleanly and reports
 * nothing wrong. {@code failed_attempts = failed_attempts + 1} is resolved by the database under a
 * row lock, so fifty attempts count as fifty. There is no {@code @Version} on {@code User} on
 * purpose: optimistic locking would cost a column and a migration to say the same thing, and would
 * turn a lost update into an exception rather than a correct count.
 *
 * <p>The lock set here is the ordinary temporary one: {@code lockedUntil} expires after
 * {@link #LOCK_DURATION} and {@code CustomUserDetailsService} reads it live, so nothing here
 * disables an account or outlives the window.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository users;

    /**
     * Counts one failed credential check and locks the account once the threshold is reached.
     *
     * <p>Both steps are single statements, and the lock decision is made by the database rather than
     * in memory: the increment cannot tell us its own result, so instead of reading the counter back
     * -- which would reintroduce exactly the race the increment just removed -- the threshold is the
     * {@code WHERE} clause of the second statement. Its update count is the answer. An attempt past
     * the threshold matches too and refreshes the window, which is the pre-existing behaviour: a
     * locked account being hammered stays locked.
     *
     * @param username the account the attempt was made against; unknown names are counted nowhere
     * @return true if the account is locked as of this attempt, which is what the caller's message
     *     to the user turns on
     */
    @Transactional
    public boolean recordFailure(String username) {
        if (users.incrementFailedAttempts(username) == 0) {
            return false;
        }
        boolean locked = users.lockIfAttemptsReached(
                username, MAX_ATTEMPTS, LocalDateTime.now().plus(LOCK_DURATION)) > 0;
        if (locked) {
            log.warn("Locked account '{}' for {} minutes after {} or more failed attempts",
                    username, LOCK_DURATION.toMinutes(), MAX_ATTEMPTS);
        }
        return locked;
    }

    /**
     * Clears the counter and any standing lock, and stamps the sign-in time.
     *
     * <p>One statement touching three columns, for the same reason the failure path is: loading the
     * user and saving it back writes every column of the row from a snapshot that a concurrent
     * request may already have moved on from.
     */
    @Transactional
    public void recordSuccess(String username) {
        users.recordSuccessfulLogin(username, LocalDateTime.now());
    }
}
