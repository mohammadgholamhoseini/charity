package com.charity.app.service;

import com.charity.app.common.error.ConflictException;
import com.charity.app.common.error.NotFoundException;
import com.charity.app.mapper.UserMapper;
import com.charity.app.model.User;
import com.charity.app.payload.UpdateAdminProfileRequest;
import com.charity.app.payload.UserProfileResponse;
import com.charity.app.repository.UserRepository;
import com.charity.app.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final UserMapper mapper;
    private final PasswordChangeGuard passwords;
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

    /**
     * The admin's own profile form, the same shape as {@code CenterService.updateOwnProfile} and
     * deliberately in the same order: the password is settled first, so a wrong current password
     * rejects the whole save with nothing applied rather than reporting only the password while the
     * email the admin corrected in the same submit disappears with the rollback.
     *
     * <p>A present-but-blank new password is rejected, not ignored, and a wrong current password is
     * counted against the login lock -- both through {@link PasswordChangeGuard}.
     */
    @Transactional
    public UserProfileResponse updateProfile(UpdateAdminProfileRequest req) {
        User user = currentUser.user();

        passwords.requireUsableNewPassword(req.newPassword());
        boolean changingPassword = req.newPassword() != null;
        if (changingPassword) {
            passwords.verifyCurrentPassword(user, req.currentPassword());
        }

        if (req.email() != null && !req.email().isBlank() && !req.email().equals(user.getEmail())) {
            if (users.existsByEmail(req.email())) {
                throw new ConflictException("EMAIL_TAKEN", "این ایمیل قبلاً ثبت شده است");
            }
            user.setEmail(req.email());
        }
        if (req.fullName() != null) {
            user.setFullName(req.fullName());
        }
        if (changingPassword) {
            passwords.applyNewPassword(user, req.newPassword());
        }

        return mapper.toProfile(users.save(user));
    }
}
