package com.charity.app.service;

import com.charity.app.common.error.ConflictException;
import com.charity.app.common.error.ValidationException;
import com.charity.app.model.enums.RequestStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.charity.app.model.enums.RequestStatus.*;

/**
 * The rules governing how a request moves between statuses.
 *
 * <p>Centralised so the transitions are stated once and can be read at a glance, rather than being
 * scattered as ad-hoc {@code if} checks across the service and two controllers.
 */
@Component
public class RequestStatusPolicy {

    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED = Map.of(
            DRAFT, EnumSet.of(PENDING, INACTIVE),
            PENDING, EnumSet.of(PUBLISHED, REJECTED, DRAFT, INACTIVE),
            REJECTED, EnumSet.of(PENDING, DRAFT, INACTIVE),
            PUBLISHED, EnumSet.of(COMPLETED, INACTIVE, REJECTED),
            COMPLETED, EnumSet.of(PUBLISHED, INACTIVE),
            INACTIVE, EnumSet.of(PENDING, PUBLISHED));

    /** Statuses whose meaning depends on an explanation the admin must supply. */
    private static final Set<RequestStatus> REQUIRES_NOTE = EnumSet.of(REJECTED);

    public void assertTransition(RequestStatus from, RequestStatus to) {
        if (from == to) {
            return;
        }
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new ConflictException("INVALID_TRANSITION",
                    "تغییر وضعیت از «%s» به «%s» مجاز نیست".formatted(from.label(), to.label()));
        }
    }

    /**
     * A rejection without a reason is useless to the centre that has to act on it, so the note is
     * mandatory. It is checked here rather than with a bean-validation annotation because whether it
     * is required depends on the target status.
     */
    public void assertNoteProvided(RequestStatus to, String note) {
        if (REQUIRES_NOTE.contains(to) && (note == null || note.isBlank())) {
            throw new ValidationException("برای رد کردن درخواست، ثبت دلیل الزامی است");
        }
    }

    /** Whether a centre may still edit a request in this status. */
    public boolean isEditableByCenter(RequestStatus status) {
        return status != COMPLETED;
    }
}
