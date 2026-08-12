package com.charity.app.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Shared by province and city create/update. */
public record NameRequest(

        @NotBlank(message = "نام الزامی است")
        @Size(max = 255)
        String name) {
}
