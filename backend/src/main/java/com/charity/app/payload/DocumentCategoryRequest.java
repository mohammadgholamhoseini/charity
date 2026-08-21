package com.charity.app.payload;

import com.charity.app.model.enums.DocumentScope;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Admin create/update for a document category.
 *
 * <p>{@code slug} may be left blank, in which case it is derived from the name. {@code scope} is
 * required and is set by the tab the admin is on; it is part of both unique keys, so the same name
 * may be used once per list.
 */
public record DocumentCategoryRequest(

        @NotNull(message = "نوع فهرست مدارک الزامی است")
        DocumentScope scope,

        @NotBlank(message = "نام دسته‌بندی الزامی است")
        @Size(max = 255)
        String name,

        @Size(max = 120)
        @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "نشانی یکتا فقط می‌تواند شامل حروف کوچک انگلیسی، عدد و خط تیره باشد")
        String slug,

        @Size(max = 500)
        String description,

        Integer sortOrder,

        boolean active) {
}
