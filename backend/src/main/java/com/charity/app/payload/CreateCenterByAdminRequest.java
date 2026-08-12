package com.charity.app.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Admin provisioning a centre together with its user account. This is the only way a centre comes
 * into existence -- there is no public registration.
 */
public record CreateCenterByAdminRequest(

        @NotBlank(message = "نام کاربری الزامی است")
        @Size(min = 3, max = 60)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "نام کاربری فقط می‌تواند شامل حروف انگلیسی، عدد، نقطه، خط تیره و زیرخط باشد")
        String username,

        @NotBlank(message = "رمز عبور موقت الزامی است")
        @Size(min = 8, max = 100, message = "رمز عبور باید حداقل ۸ نویسه باشد")
        String password,

        @NotBlank(message = "ایمیل الزامی است")
        @Email(message = "قالب ایمیل معتبر نیست")
        @Size(max = 255)
        String email,

        @NotBlank(message = "نام مرکز الزامی است")
        @Size(max = 255)
        String centerName,

        @Size(max = 255)
        String fullName,

        @NotNull(message = "انتخاب شهر الزامی است")
        Long cityId,

        @NotEmpty(message = "حداقل یک دسته‌بندی مجاز باید انتخاب شود")
        @Size(max = 30)
        List<@NotNull Long> categoryIds,

        @Size(max = 1000)
        String description,

        @Size(max = 255)
        String contactPhone,

        @Size(max = 120)
        String responseHours,

        @Size(max = 1000)
        String address,

        @Size(max = 255)
        String cardNumber,

        @Size(max = 255)
        String sheba,

        Boolean active) {
}
