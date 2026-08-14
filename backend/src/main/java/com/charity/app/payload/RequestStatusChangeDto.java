package com.charity.app.payload;

import com.charity.app.model.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * An admin moving a request to a new status.
 *
 * <p>The note is mandatory for INACTIVE, but that is enforced in {@code RequestStatusPolicy} rather
 * than by an annotation here, because whether it is required depends on the target status.
 */
public record RequestStatusChangeDto(

        @NotNull(message = "انتخاب وضعیت الزامی است")
        RequestStatus status,

        @Size(max = 1000, message = "یادداشت نمی‌تواند بیش از ۱۰۰۰ نویسه باشد")
        String note) {
}
