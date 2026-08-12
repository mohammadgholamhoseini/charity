package com.charity.app.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Replaces the bare Map<String,String> body the logo endpoint used to accept unvalidated. */
public record SetLogoDto(

        @NotBlank(message = "نشانی لوگو الزامی است")
        @Size(max = 255)
        String logoUrl) {
}
