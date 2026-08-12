package com.charity.app.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAdminProfileRequest(

        @Size(max = 255)
        String fullName,

        @Email(message = "قالب ایمیل معتبر نیست")
        @Size(max = 255)
        String email,

        @Size(max = 200)
        String currentPassword,

        @Size(min = 8, max = 100, message = "رمز عبور جدید باید حداقل ۸ نویسه باشد")
        String newPassword) {
}
