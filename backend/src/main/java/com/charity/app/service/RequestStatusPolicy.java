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
 *
 * <p>There is no approval step. A centre publishes its own requests, so nothing routes through
 * PENDING and nothing is REJECTED; both constants survive only because rows in the production
 * database still carry them and an enum that cannot read its own table is worse than a dead value.
 * They are reachable from nowhere here, which is the point -- the map is the whole workflow.
 */
@Component
public class RequestStatusPolicy {

    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED = Map.of(
            DRAFT, EnumSet.of(PUBLISHED, INACTIVE),
            PUBLISHED, EnumSet.of(COMPLETED, INACTIVE),
            COMPLETED, EnumSet.of(PUBLISHED, INACTIVE),
            INACTIVE, EnumSet.of(PUBLISHED));

    /** Statuses whose meaning depends on an explanation the admin must supply. */
    private static final Set<RequestStatus> REQUIRES_NOTE = EnumSet.of(INACTIVE);

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
     * Taking a live request down is the one destructive thing an admin can do to a centre's work, so
     * it has to come with a reason the centre can read. Checked here rather than with a
     * bean-validation annotation because whether it is required depends on the target status.
     */
    public void assertNoteProvided(RequestStatus to, String note) {
        if (REQUIRES_NOTE.contains(to) && (note == null || note.isBlank())) {
            throw new ValidationException("برای غیرفعال کردن درخواست، ثبت دلیل الزامی است");
        }
    }

    /** Whether a centre may still edit a request in this status. */
    public boolean isEditableByCenter(RequestStatus status) {
        return status != COMPLETED;
    }
}
