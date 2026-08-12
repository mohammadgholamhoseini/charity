package com.charity.app.payload;

import com.charity.app.model.enums.Urgency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * What a centre or admin submits to edit an existing request.
 *
 * <p>Editing a PUBLISHED request deliberately does <em>not</em> send it back for re-moderation:
 * re-approving every typo fix would repeatedly de-index a live URL. The admin list instead flags
 * rows whose {@code updatedAt} is later than their {@code publishedAt}.
 */
public record RequestUpdateDto(

        @NotBlank(message = "عنوان درخواست الزامی است")
        @Size(max = 255, message = "عنوان درخواست نمی‌تواند بیش از ۲۵۵ نویسه باشد")
        String title,

        @NotNull(message = "انتخاب دسته‌بندی الزامی است")
        Long categoryId,

        @NotNull(message = "انتخاب شهر الزامی است")
        Long cityId,

        @Size(max = 3000, message = "شرح نیاز نمی‌تواند بیش از ۳۰۰۰ نویسه باشد")
        String description,

        @NotNull(message = "مبلغ مورد نیاز الزامی است")
        @DecimalMin(value = "0.0", inclusive = false, message = "مبلغ مورد نیاز باید بزرگ‌تر از صفر باشد")
        BigDecimal amountNeeded,

        LocalDate deadline,

        Urgency urgency,

        @Size(max = 255)
        String imageUrl,

        @Size(max = 500, message = "اطلاعات تماس نمی‌تواند بیش از ۵۰۰ نویسه باشد")
        String contactInfo,

        Map<String, Object> details,

        @Size(max = 70, message = "عنوان سئو نمی‌تواند بیش از ۷۰ نویسه باشد")
        String metaTitle,

        @Size(max = 160, message = "توضیح سئو نمی‌تواند بیش از ۱۶۰ نویسه باشد")
        String metaDescription) {
}
