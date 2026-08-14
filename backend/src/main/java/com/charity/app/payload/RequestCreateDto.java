package com.charity.app.payload;

import com.charity.app.model.enums.Urgency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

/**
 * What a centre submits to create a request.
 *
 * <p>{@code categoryId} is additionally checked against the centre's allowed categories in the
 * service -- a centre may only publish in categories an admin granted it.
 *
 * <p>{@code submit} distinguishes the panel's two buttons: «ذخیره پیش‌نویس» leaves the request in
 * DRAFT, «انتشار درخواست» publishes it immediately. There is no admin approval step.
 */
public record RequestCreateDto(

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

        /** false -> save as DRAFT, true -> publish now. */
        boolean submit) {
}
