package com.charity.app.security;

import com.charity.app.model.User;
import com.charity.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("کاربر یافت نشد"));

        // Reporting the lock here means DaoAuthenticationProvider rejects the attempt in its
        // pre-authentication checks, before the password is compared -- so a locked account cannot
        // be probed for a correct password. Previously accountNonLocked was hardcoded true.
        boolean locked = user.isLockedAt(LocalDateTime.now());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole().authority())))
                .disabled(!user.isEnabled())
                .accountLocked(locked)
                .accountExpired(false)
                .credentialsExpired(false)
                .build();
    }
}
