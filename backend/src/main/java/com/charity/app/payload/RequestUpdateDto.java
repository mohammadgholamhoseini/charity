package com.charity.app.payload;

import com.charity.app.model.enums.Urgency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

/**
 * What a centre or admin submits to edit an existing request.
 *
 * <p>Editing never changes the status. That mattered more when an admin had to approve each
 * request; now that publication is immediate it simply means a typo fix cannot take a live URL
 * out of the index.
 */
public record RequestUpdateDto(

        @NotBlank(message = "عنوان درخواست الزامی است")
        @Size(max = 255, message = "عنوان درخواست نمی‌تواند بیش از ۲۵۵ نویسه باشد")
        String title,

        @NotNull(message = "انتخاب دسته‌بندی الزامی است")
        Long categoryId,

        @Size(max = 3000, message = "شرح نیاز نمی‌تواند بیش از ۳۰۰۰ نویسه باشد")
        String description,

        @NotNull(message = "مبلغ مورد نیاز الزامی است")
        @DecimalMin(value = "0.0", inclusive = false, message = "مبلغ مورد نیاز باید بزرگ‌تر از صفر باشد")
        BigDecimal amountNeeded,

        Urgency urgency,

        @Size(max = 255)
        String imageUrl,

        Map<String, Object> details,

        @Size(max = 70, message = "عنوان سئو نمی‌تواند بیش از ۷۰ نویسه باشد")
        String metaTitle,

        @Size(max = 160, message = "توضیح سئو نمی‌تواند بیش از ۱۶۰ نویسه باشد")
        String metaDescription) {
}
