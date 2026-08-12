package com.charity.app.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Admin editing any centre, including which categories it may publish in. */
public record UpdateCenterByAdminRequest(

        @NotBlank(message = "نام مرکز الزامی است")
        @Size(max = 255)
        String centerName,

        @Size(max = 255)
        String fullName,

        @NotNull(message = "انتخاب شهر الزامی است")
        Long cityId,

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
