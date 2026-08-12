package com.charity.app.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A centre editing its own profile. Deliberately narrower than the admin form: a centre cannot
 * change which categories it is allowed to publish in, nor its own active status.
 */
public record UpdateCenterProfileRequest(

        @NotBlank(message = "نام مرکز الزامی است")
        @Size(max = 255)
        String centerName,

        @Size(max = 255)
        String fullName,

        Long cityId,

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

        @Size(max = 255)
        String logoUrl) {
}
