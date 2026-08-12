package com.charity.app.payload;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Replaces the bare List<Long> body the admin categories endpoint used to accept unvalidated. */
public record SetCategoriesDto(

        @NotEmpty(message = "حداقل یک دسته‌بندی باید انتخاب شود")
        @Size(max = 30, message = "حداکثر ۳۰ دسته‌بندی قابل انتخاب است")
        List<@NotNull Long> categoryIds) {
}
