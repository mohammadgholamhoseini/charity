package com.charity.app.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Admin create/update for a category.
 *
 * <p>{@code slug} may be left blank, in which case it is derived from the name. The two label
 * colours come from the eight swatches in the admin form.
 */
public record CategoryRequest(

        @NotBlank(message = "نام دسته‌بندی الزامی است")
        @Size(max = 255)
        String name,

        @Size(max = 120)
        @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "نشانی یکتا فقط می‌تواند شامل حروف کوچک انگلیسی، عدد و خط تیره باشد")
        String slug,

        @Size(max = 500)
        String description,

        @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "رنگ باید به قالب #RRGGBB باشد")
        String labelBg,

        @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "رنگ باید به قالب #RRGGBB باشد")
        String labelText,

        Integer sortOrder,

        @Size(max = 255)
        String iconUrl,

        boolean active) {
}
