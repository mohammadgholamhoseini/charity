package com.charity.app.security;

import com.charity.app.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    /**
     * Secrets that have been published in this repository's history and must never sign a token
     * again, regardless of length.
     */
    private static final Set<String> KNOWN_COMPROMISED = Set.of(
            "a-very-long-random-string-at-least",
            "changeThisSecretKeyToAVeryLongRandomStringAtLeast256BitsLongForProduction");

    private static final int MIN_SECRET_BYTES = 32;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Refuses to start on a missing, short or previously-published secret.
     *
     * <p>The old configuration fell back to a 32-byte literal committed to a public repository. That
     * is a perfectly valid HS256 key, so anyone who read the source could sign {@code {"sub":"admin"}}
     * and hold full admin rights. Crashing at startup is the only safe response to an unset secret.
     */
    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Provide a random secret of at least 32 bytes.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long.");
        }
        if (KNOWN_COMPROMISED.contains(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET is a value published in source control. Generate a new one.");
        }
    }

    public String generateToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getUsername())
                // Role and id are carried as claims for convenience, but authorisation still reloads
                // the user on every request -- that reload is what makes a lock or a disable take
                // effect before the token would otherwise expire.
                .claim("role", user.getRole().name())
                .claim("uid", user.getId())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(signingKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return userDetails.getUsername().equals(extractUsername(token)) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
