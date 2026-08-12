package com.charity.app.service;

import com.charity.app.common.error.ConflictException;
import com.charity.app.common.error.NotFoundException;
import com.charity.app.common.error.ValidationException;
import com.charity.app.mapper.UserMapper;
import com.charity.app.model.User;
import com.charity.app.payload.UpdateAdminProfileRequest;
import com.charity.app.payload.UserProfileResponse;
import com.charity.app.repository.UserRepository;
import com.charity.app.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("کاربر یافت نشد"));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse currentProfile() {
        return mapper.toProfile(currentUser.user());
    }

    @Transactional
    public UserProfileResponse updateProfile(UpdateAdminProfileRequest req) {
        User user = currentUser.user();

        if (req.email() != null && !req.email().isBlank() && !req.email().equals(user.getEmail())) {
            if (users.existsByEmail(req.email())) {
                throw new ConflictException("EMAIL_TAKEN", "این ایمیل قبلاً ثبت شده است");
            }
            user.setEmail(req.email());
        }
        if (req.fullName() != null) {
            user.setFullName(req.fullName());
        }
        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            if (req.currentPassword() == null
                    || !passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
                throw new ValidationException("رمز عبور فعلی نادرست است");
            }
            user.setPassword(passwordEncoder.encode(req.newPassword()));
            // Changing the password clears any standing lock.
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
        }

        return mapper.toProfile(users.save(user));
    }
}
