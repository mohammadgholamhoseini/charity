package com.charity.app.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * An admin setting a new password for a centre's account, typically because the centre lost it or
 * locked itself out. The admin types the password; nothing is generated and nothing is echoed back.
 *
 * <p>The constraints and messages are copied verbatim from
 * {@link CreateCenterByAdminRequest#password()} so the two ways a centre's password gets set cannot
 * drift apart.
 */
public record ResetCenterPasswordRequest(

        @NotBlank(message = "رمز عبور موقت الزامی است")
        @Size(min = 8, max = 100, message = "رمز عبور باید حداقل ۸ نویسه باشد")
        String newPassword) {
}
