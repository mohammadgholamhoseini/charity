package com.charity.app.service;

import com.charity.app.common.error.NotFoundException;
import com.charity.app.model.User;
import com.charity.app.payload.AuthRequest;
import com.charity.app.payload.AuthResponse;
import com.charity.app.repository.CenterRepository;
import com.charity.app.repository.UserRepository;
import com.charity.app.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final CenterRepository centers;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Authentication failures propagate as Spring Security exceptions and are translated to a 401
     * with a single generic message by the global handler. They used to reach the catch-all handler
     * and come back as a 500.
     *
     * <p>Lock state is applied by {@code CustomUserDetailsService} before the password is even
     * checked, and the attempt counters are maintained by {@link LoginAttemptService}, which
     * {@code AuthEventListener} drives from the success and failure events this call publishes.
     */
    public AuthResponse authenticate(AuthRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));

        User user = users.findByUsername(req.username())
                .orElseThrow(() -> new NotFoundException("کاربر یافت نشد"));

        Long centerId = centers.findByUserId(user.getId()).map(c -> c.getId()).orElse(null);
        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getRole(),
                user.getId(),
                user.getFullName(),
                centerId);
    }
}
