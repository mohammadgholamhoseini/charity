package com.charity.app.service;

import com.charity.app.model.User;
import com.charity.app.model.enums.UserRole;
import com.charity.app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The login throttle, against a real database.
 *
 * <p>Mocking the repository here would test nothing worth testing. The whole design of
 * {@link LoginAttemptService} is that the counting and the lock decision happen <i>in SQL</i> --
 * {@code failed_attempts = failed_attempts + 1} under a row lock, and a threshold expressed as the
 * {@code WHERE} clause of the second statement rather than as an {@code if} in Java. A stubbed
 * repository would assert only that the service calls two methods, which is the one thing that was
 * never in doubt.
 *
 * <p>Each test rolls back, so the seeded admin the rest of the suite counts on is untouched.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
@DisplayName("the login throttle behind the five-attempt promise on the login screen")
class LoginAttemptServiceTest {

    private static final String USERNAME = "throttle-subject";

    @Autowired private LoginAttemptService attempts;
    @Autowired private UserRepository users;
    @Autowired private EntityManager em;

    @BeforeEach
    void createSubject() {
        users.save(User.builder()
                .username(USERNAME)
                .email(USERNAME + "@test.local")
                .password("irrelevant")
                .role(UserRole.CENTER)
                .enabled(true)
                .failedAttempts(0)
                .build());
        // The counting statements are bulk updates with no automatic flush; without this the INSERT
        // would still be pending when the first UPDATE reaches the database.
        em.flush();
    }

    /** Bulk updates bypass the persistence context, so the cached entity has to be discarded. */
    private User reread() {
        em.flush();
        em.clear();
        return users.findByUsername(USERNAME).orElseThrow();
    }

    @Test
    @DisplayName("four failures count but do not lock")
    void fourFailuresDoNotLock() {
        for (int i = 1; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            assertThat(attempts.recordFailure(USERNAME)).as("attempt %d", i).isFalse();
        }

        User user = reread();
        assertThat(user.getFailedAttempts()).isEqualTo(LoginAttemptService.MAX_ATTEMPTS - 1);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("the fifth failure locks the account, and says so to its caller")
    void fifthFailureLocks() {
        // The return value is what the caller turns into the user-visible message, so it is part of
        // the contract rather than a convenience.
        boolean locked = false;
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            locked = attempts.recordFailure(USERNAME);
        }

        assertThat(locked).isTrue();
        assertThat(reread().getLockedUntil())
                .isNotNull()
                .isAfter(LocalDateTime.now().plus(LoginAttemptService.LOCK_DURATION).minusMinutes(1));
    }

    @Test
    @DisplayName("hammering a locked account refreshes the window rather than resetting it")
    void furtherAttemptsKeepItLocked() {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            attempts.recordFailure(USERNAME);
        }
        LocalDateTime firstLock = reread().getLockedUntil();

        assertThat(attempts.recordFailure(USERNAME))
                .as("an attempt past the threshold still reports the account as locked")
                .isTrue();
        assertThat(reread().getLockedUntil()).isAfterOrEqualTo(firstLock);
    }

    @Test
    @DisplayName("a successful sign-in clears the counter and the lock together")
    void successClearsEverything() {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            attempts.recordFailure(USERNAME);
        }
        assertThat(reread().getLockedUntil()).isNotNull();

        attempts.recordSuccess(USERNAME);

        User user = reread();
        assertThat(user.getFailedAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("an unknown username is counted nowhere and locks nothing")
    void unknownUsernameIsANoOp() {
        // Otherwise the throttle would be a way to discover which accounts exist, and a way to lock
        // an account out by guessing at names.
        assertThat(attempts.recordFailure("no-such-account")).isFalse();

        User untouched = reread();
        assertThat(untouched.getFailedAttempts()).isZero();
        assertThat(untouched.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("the counter is per-account: one account's failures do not lock another")
    void countingIsPerAccount() {
        users.save(User.builder()
                .username("bystander")
                .email("bystander@test.local")
                .password("irrelevant")
                .role(UserRole.CENTER)
                .enabled(true)
                .failedAttempts(0)
                .build());
        em.flush();

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            attempts.recordFailure(USERNAME);
        }

        em.clear();
        User bystander = users.findByUsername("bystander").orElseThrow();
        assertThat(bystander.getFailedAttempts()).isZero();
        assertThat(bystander.getLockedUntil()).isNull();
    }
}
