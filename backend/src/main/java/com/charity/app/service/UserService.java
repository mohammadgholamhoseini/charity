package com.charity.app.service;

import com.charity.app.model.User;
import com.charity.app.payload.UpdateAdminProfileRequest;
import com.charity.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("کاربر یافت نشد"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> currentProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("کاربر یافت نشد"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("fullName", user.getFullName());
        map.put("role", user.getRole().name());
        return map;
    }

    @Transactional
    public Map<String, Object> updateProfile(UpdateAdminProfileRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("کاربر یافت نشد"));

        if (req.getEmail() != null && !req.getEmail().isBlank()
                && !req.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new IllegalArgumentException("ایمیل قبلاً ثبت شده است");
            }
            user.setEmail(req.getEmail());
        }
        if (req.getFullName() != null) user.setFullName(req.getFullName());

        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            if (req.getCurrentPassword() == null
                    || !passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("رمز عبور فعلی اشتباه است");
            }
            user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        }

        user = userRepository.save(user);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("fullName", user.getFullName());
        map.put("role", user.getRole().name());
        return map;
    }
}
