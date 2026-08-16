package com.charity.app.service;

import com.charity.app.common.error.ConflictException;
import com.charity.app.common.error.ValidationException;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.UserRole;
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
 * <p>The two maps differ in exactly one row -- what may follow COMPLETED. Everything else a centre
 * can do to its own listing, an admin can do too.
 *
 * <p>There is no approval step. A centre publishes its own requests, so nothing routes through
 * PENDING and nothing is REJECTED; both constants survive only because rows in the production
 * database still carry them and an enum that cannot read its own table is worse than a dead value.
 * They are reachable from nowhere here, which is the point -- the map is the whole workflow.
 */
@Component
public class RequestStatusPolicy {

    private static final Map<RequestStatus, Set<RequestStatus>> ADMIN_ALLOWED = Map.of(
            DRAFT, EnumSet.of(PUBLISHED, INACTIVE),
            PUBLISHED, EnumSet.of(COMPLETED, INACTIVE),
            // The one way out of COMPLETED, and it is a takedown rather than a reopening. A
            // completed request that turns out to be fraudulent or plain wrong still has to be
            // removable from the site; without this the only remedy would be hand-written SQL.
            COMPLETED, EnumSet.of(INACTIVE),
            INACTIVE, EnumSet.of(PUBLISHED));

    private static final Map<RequestStatus, Set<RequestStatus>> CENTER_ALLOWED = Map.of(
            DRAFT, EnumSet.of(PUBLISHED, INACTIVE),
            PUBLISHED, EnumSet.of(COMPLETED, INACTIVE),
            // Terminal. A completed request is the record that its need was met, and republishing
            // it would put a listing back in front of donors for something already paid for.
            // A centre that completes one by mistake has to ask an admin.
            COMPLETED, Set.of(),
            INACTIVE, EnumSet.of(PUBLISHED));

    /** Statuses whose meaning depends on an explanation the admin must supply. */
    private static final Set<RequestStatus> REQUIRES_NOTE = EnumSet.of(INACTIVE);

    public void assertTransition(RequestStatus from, RequestStatus to, UserRole actor) {
        if (from == to) {
            return;
        }
        if (!allowed(actor).getOrDefault(from, Set.of()).contains(to)) {
            throw new ConflictException("INVALID_TRANSITION",
                    "تغییر وضعیت از «%s» به «%s» مجاز نیست".formatted(from.label(), to.label()));
        }
    }

    private static Map<RequestStatus, Set<RequestStatus>> allowed(UserRole actor) {
        return actor == UserRole.CENTER ? CENTER_ALLOWED : ADMIN_ALLOWED;
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

    /**
     * Whether a centre may delete a request in this status.
     *
     * <p>A completed request is a record of work done, and the centre that did it is the last party
     * who should be able to erase it. An admin still can -- deletion is soft, so the row survives
     * either way and the URL answers 410 rather than vanishing.
     */
    public boolean isDeletableByCenter(RequestStatus status) {
        return status != COMPLETED;
    }
}
