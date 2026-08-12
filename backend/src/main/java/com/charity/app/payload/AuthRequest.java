package com.charity.app.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(

        @NotBlank(message = "نام کاربری الزامی است")
        @Size(max = 100)
        String username,

        @NotBlank(message = "رمز عبور الزامی است")
        @Size(max = 200)
        String password) {
}
