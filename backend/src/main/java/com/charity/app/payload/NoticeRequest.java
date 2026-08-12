package com.charity.app.payload;

import com.charity.app.model.enums.NoticePlacement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Admin create/update for an announcement.
 *
 * <p>The 90-character title cap matches the admin form's own counter -- the top banner is a single
 * line and longer titles simply do not fit.
 */
public record NoticeRequest(

        @NotBlank(message = "عنوان اطلاعیه الزامی است")
        @Size(max = 90, message = "عنوان اطلاعیه نمی‌تواند بیش از ۹۰ نویسه باشد")
        String title,

        @NotBlank(message = "متن اطلاعیه الزامی است")
        @Size(max = 4000)
        String content,

        @NotNull(message = "انتخاب محل نمایش الزامی است")
        NoticePlacement placement,

        LocalDateTime startAt,

        LocalDateTime endAt,

        @Size(max = 500)
        String linkUrl,

        boolean active) {
}
