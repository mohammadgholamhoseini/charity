package com.charity.app.repository;

import com.charity.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /**
     * Adds one to the counter inside the database, under the row lock the {@code UPDATE} takes, so
     * that concurrent failed attempts count as many instead of as one. Loading the user, adding one
     * in memory and saving it back is a read-modify-write and loses every attempt but the last --
     * which is a throttle that does not throttle. Only {@link com.charity.app.service.LoginAttemptService}
     * should call this; see that class for the transaction it has to be reached through, and for why
     * no caller may already hold one.
     *
     * @return 1 if the account exists and was counted, 0 if there is no such username
     */
    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = u.failedAttempts + 1 WHERE u.username = :username")
    int incrementFailedAttempts(@Param("username") String username);

    /**
     * Sets the temporary lock if the counter has reached the threshold, deciding that in the
     * {@code WHERE} clause rather than in Java. The increment above cannot return its own result, and
     * reading the counter back to compare it would put the lost-update race straight back into the
     * lock decision; the update count of this statement answers the same question without a read.
     *
     * @return 1 if the account is locked as of now, 0 if it is still below the threshold or absent
     */
    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = :until "
            + "WHERE u.username = :username AND u.failedAttempts >= :threshold")
    int lockIfAttemptsReached(@Param("username") String username,
                              @Param("threshold") int threshold,
                              @Param("until") LocalDateTime until);

    /** Clears the counter and the lock and stamps the sign-in, without rewriting the whole row. */
    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = 0, u.lockedUntil = NULL, u.lastLoginAt = :now "
            + "WHERE u.username = :username")
    int recordSuccessfulLogin(@Param("username") String username, @Param("now") LocalDateTime now);
}
