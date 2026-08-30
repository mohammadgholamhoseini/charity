package com.charity.app.security;

import com.charity.app.model.User;
import com.charity.app.model.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The startup guard on {@code JWT_SECRET}, and the token round-trip.
 *
 * <p>The guard is the part worth pinning. The previous configuration fell back to a 32-byte literal
 * committed to this repository, which is a perfectly valid HS256 key -- anyone who read the source
 * could sign {@code {"sub":"admin"}} and hold full admin rights. Refusing to start is the only safe
 * response, and "refuses to start" is a behaviour that is very easy to soften back into a warning.
 */
class JwtServiceTest {

    /** 63 bytes; comfortably over the minimum and not a value that has ever been published. */
    private static final String GOOD_SECRET =
            "test-only-secret-that-is-long-enough-for-hs256-and-never-shipped";

    private static final long ONE_HOUR = 3_600_000L;

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = configured(GOOD_SECRET, ONE_HOUR);
    }

    private static JwtService configured(String secret, long expirationMs) {
        JwtService jwt = new JwtService();
        // @Value fields, so there is no constructor to hand these to.
        ReflectionTestUtils.setField(jwt, "secret", secret);
        ReflectionTestUtils.setField(jwt, "expirationMs", expirationMs);
        return jwt;
    }

    private static User user(String username, UserRole role) {
        return User.builder().id(42L).username(username).password("x").role(role).build();
    }

    private static UserDetails details(String username) {
        return org.springframework.security.core.userdetails.User
                .withUsername(username).password("x").authorities("ROLE_ADMIN").build();
    }

    @Nested
    @DisplayName("the secret is validated at startup, not at first use")
    class SecretGuard {

        @Test
        @DisplayName("an unset secret crashes the application")
        void unsetIsFatal() {
            assertThatThrownBy(() -> configured(null, ONE_HOUR).validateSecret())
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> configured("", ONE_HOUR).validateSecret())
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> configured("   ", ONE_HOUR).validateSecret())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("a secret shorter than 32 bytes is refused")
        void tooShortIsFatal() {
            String thirtyOne = "a".repeat(31);
            assertThatThrownBy(() -> configured(thirtyOne, ONE_HOUR).validateSecret())
                    .isInstanceOf(IllegalStateException.class);

            assertThatCode(() -> configured("a".repeat(32), ONE_HOUR).validateSecret())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("length is counted in bytes, not characters")
        void lengthIsInBytes() {
            // A 32-character Persian string is 64 bytes; a 16-character one is 32 and only just
            // passes. Counting characters would let a much weaker key through.
            String sixteenPersianChars = "کلید".repeat(4);
            assertThat(sixteenPersianChars).hasSize(16);
            assertThatCode(() -> configured(sixteenPersianChars, ONE_HOUR).validateSecret())
                    .doesNotThrowAnyException();

            String fifteenPersianChars = sixteenPersianChars.substring(0, 15);
            assertThatThrownBy(() -> configured(fifteenPersianChars, ONE_HOUR).validateSecret())
                    .isInstanceOf(IllegalStateException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "a-very-long-random-string-at-least",
                "changeThisSecretKeyToAVeryLongRandomStringAtLeast256BitsLongForProduction",
        })
        @DisplayName("a secret published in this repository's history is refused whatever its length")
        void publishedSecretsAreRefused(String compromised) {
            assertThat(compromised.length())
                    .as("these are long enough to pass the length check, which is the point")
                    .isGreaterThanOrEqualTo(32);
            assertThatThrownBy(() -> configured(compromised, ONE_HOUR).validateSecret())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("a fresh secret of sufficient length is accepted")
        void goodSecretPasses() {
            assertThatCode(() -> service.validateSecret()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("tokens")
    class Tokens {

        @Test
        @DisplayName("the subject survives a round trip")
        void roundTripsTheUsername() {
            String token = service.generateToken(user("centre-1", UserRole.CENTER));

            assertThat(service.extractUsername(token)).isEqualTo("centre-1");
        }

        @Test
        @DisplayName("role and id ride along as claims")
        void carriesRoleAndId() {
            String token = service.generateToken(user("admin", UserRole.ADMIN));

            String role = service.extractClaim(token, claims -> claims.get("role", String.class));
            Integer uid = service.extractClaim(token, claims -> claims.get("uid", Integer.class));

            assertThat(role).isEqualTo("ADMIN");
            assertThat(uid).isEqualTo(42);
        }

        @Test
        @DisplayName("two tokens for the same user are still distinct")
        void everyTokenHasItsOwnId() {
            // A jti means one leaked token can be reasoned about on its own.
            String first = service.generateToken(user("admin", UserRole.ADMIN));
            String second = service.generateToken(user("admin", UserRole.ADMIN));

            String firstId = service.extractClaim(first, Claims::getId);
            String secondId = service.extractClaim(second, Claims::getId);

            assertThat(firstId).isNotBlank().isNotEqualTo(secondId);
        }

        @Test
        @DisplayName("a token is valid only for the user it was minted for")
        void boundToItsSubject() {
            String token = service.generateToken(user("centre-1", UserRole.CENTER));

            assertThat(service.isTokenValid(token, details("centre-1"))).isTrue();
            assertThat(service.isTokenValid(token, details("centre-2"))).isFalse();
        }

        @Test
        @DisplayName("an expired token is not valid")
        void expiryIsEnforced() {
            JwtService alreadyExpired = configured(GOOD_SECRET, -1_000L);
            String token = alreadyExpired.generateToken(user("centre-1", UserRole.CENTER));

            assertThatThrownBy(() -> alreadyExpired.isTokenValid(token, details("centre-1")))
                    .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
        }

        @Test
        @DisplayName("a token signed with a different secret is rejected, not merely unparsed")
        void signatureIsVerified() {
            // This is the whole reason the published-secret guard exists: without verification, a
            // forged token would read back cleanly.
            String foreign = configured("a-completely-different-but-equally-long-secret-value", ONE_HOUR)
                    .generateToken(user("admin", UserRole.ADMIN));

            assertThatThrownBy(() -> service.extractUsername(foreign))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("a tampered payload invalidates the signature")
        void tamperingIsDetected() {
            String token = service.generateToken(user("centre-1", UserRole.CENTER));
            String[] parts = token.split("\\.");
            String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"sub\":\"admin\",\"role\":\"ADMIN\"}".getBytes());
            String forged = parts[0] + "." + forgedPayload + "." + parts[2];

            assertThatThrownBy(() -> service.extractUsername(forged))
                    .isInstanceOf(io.jsonwebtoken.JwtException.class);
        }
    }
}
