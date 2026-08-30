package com.charity.app.service;

import com.charity.app.common.AppUrls;
import com.charity.app.common.error.ConflictException;
import com.charity.app.common.error.ForbiddenException;
import com.charity.app.common.error.GoneException;
import com.charity.app.common.error.NotFoundException;
import com.charity.app.common.error.ValidationException;
import com.charity.app.event.RequestPublishedEvent;
import com.charity.app.mapper.RequestMapper;
import com.charity.app.model.Category;
import com.charity.app.model.Center;
import com.charity.app.model.Request;
import com.charity.app.model.enums.CenterStatus;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.UserRole;
import com.charity.app.payload.RequestFilter;
import com.charity.app.payload.RequestStatusChangeDto;
import com.charity.app.payload.RequestUpdateDto;
import com.charity.app.repository.CategoryRepository;
import com.charity.app.repository.CenterRepository;
import com.charity.app.repository.CityRepository;
import com.charity.app.repository.RequestRepository;
import com.charity.app.repository.RequestSlugHistoryRepository;
import com.charity.app.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The domain rules that live in {@link RequestService} rather than in the status matrix: who owns a
 * request, what a soft delete does to its URL, and whether a centre can walk back a moderation
 * decision.
 *
 * <p>The status policy is the real object, not a mock. Half of what is being asserted here is the
 * <i>interaction</i> between the service's ownership checks and the policy's transition table, and
 * a stubbed policy would assert only that the service calls something.
 *
 * <p>Everything else is mocked, so nothing here touches a database. That is deliberate: these are
 * decisions made in Java, and testing them through Hibernate would only add ways to fail for
 * reasons that have nothing to do with the rule under test.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestServiceTest {

    @Mock private RequestRepository requests;
    @Mock private RequestSlugHistoryRepository slugHistory;
    @Mock private CenterRepository centers;
    @Mock private CategoryRepository categories;
    @Mock private CityRepository cities;
    @Mock private RequestMapper mapper;
    @Mock private CurrentUser currentUser;
    @Mock private ApplicationEventPublisher events;

    /** The real policy: the point of most of these tests is how the service and it compose. */
    @Spy private RequestStatusPolicy statusPolicy = new RequestStatusPolicy();

    /** Plain constructor, no Spring needed; only {@code zone()} is ever reached from here. */
    @Spy private AppUrls urls = new AppUrls("http://api.test", "http://site.test", "Asia/Tehran");

    @InjectMocks private RequestService service;

    private static final long OWN_CENTER_ID = 7L;
    private static final long OTHER_CENTER_ID = 8L;

    private Center ownCenter;

    @BeforeEach
    void setUp() {
        ownCenter = center(OWN_CENTER_ID);
        when(currentUser.center()).thenReturn(ownCenter);
        // Saving returns what it was given, so a test can read the state the service wrote.
        when(requests.save(any(Request.class))).thenAnswer(call -> call.getArgument(0));
    }

    // ------------------------------------------------------------------ fixtures

    private static Center center(long id) {
        return Center.builder()
                .id(id)
                .name("centre-" + id)
                .status(CenterStatus.APPROVED)
                .categories(new HashSet<>())
                .build();
    }

    private static Request request(long id, Center owner, RequestStatus status) {
        return Request.builder()
                .id(id)
                .code("RQ-" + (1000 + id))
                .slug("request-rq-" + (1000 + id))
                .center(owner)
                .title("a request")
                .amountNeeded(new BigDecimal("1000000"))
                .status(status)
                .build();
    }

    /** A request an admin has taken down, exactly as {@code applyStatus} would leave it. */
    private Request takenDownByAdmin(long id) {
        Request request = request(id, ownCenter, RequestStatus.INACTIVE);
        request.setDeactivatedBy(UserRole.ADMIN);
        request.setStatusNote("removed by an admin for a stated reason");
        return request;
    }

    private void existing(Request request) {
        when(requests.findById(request.getId())).thenReturn(Optional.of(request));
    }

    // ------------------------------------------------------------------ tests

    @Nested
    @DisplayName("a request belongs to exactly one centre")
    class Ownership {

        @Test
        @DisplayName("a centre cannot touch another centre's request")
        void foreignRequestIsForbidden() {
            Request foreign = request(1L, center(OTHER_CENTER_ID), RequestStatus.PUBLISHED);
            existing(foreign);

            assertThatThrownBy(() -> service.centerDetail(1L)).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> service.publishByCenter(1L)).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> service.softDeleteByCenter(1L)).isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("a soft-deleted request is invisible even to its owner's panel")
        void deletedIsGoneFromThePanel() {
            Request deleted = request(1L, ownCenter, RequestStatus.PUBLISHED);
            deleted.setDeletedAt(LocalDateTime.now());
            existing(deleted);

            assertThatThrownBy(() -> service.centerDetail(1L)).isInstanceOf(NotFoundException.class);
            assertThatThrownBy(() -> service.adminDetail(1L)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("an admin's takedown outranks the centre")
    class AdminTakedown {

        @Test
        @DisplayName("the centre cannot publish over it")
        void centreCannotRepublish() {
            existing(takenDownByAdmin(1L));

            assertThatThrownBy(() -> service.publishByCenter(1L))
                    .isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> service.changeStatusByCenter(
                    1L, new RequestStatusChangeDto(RequestStatus.PUBLISHED, null)))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("the centre cannot mark it completed either")
        void centreCannotComplete() {
            existing(takenDownByAdmin(1L));

            assertThatThrownBy(() -> service.markCompletedByCenter(1L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("the admin who took it down can put it back")
        void adminCanRestore() {
            existing(takenDownByAdmin(1L));

            service.changeStatus(1L, new RequestStatusChangeDto(RequestStatus.PUBLISHED, null));

            verify(requests).save(any(Request.class));
        }

        @Test
        @DisplayName("a centre's own withdrawal is its own to reverse")
        void centreMayUndoItsOwnWithdrawal() {
            Request withdrawn = request(1L, ownCenter, RequestStatus.INACTIVE);
            withdrawn.setDeactivatedBy(UserRole.CENTER);
            existing(withdrawn);

            assertThatCode(() -> service.publishByCenter(1L)).doesNotThrowAnyException();
            assertThat(withdrawn.getStatus()).isEqualTo(RequestStatus.PUBLISHED);
        }

        @Test
        @DisplayName("the actor flag is cleared on the way out of INACTIVE")
        void flagIsClearedWhenRestored() {
            // Left set, a later withdrawal by the centre would look like an admin's and lock the
            // centre out of its own request.
            Request restored = takenDownByAdmin(1L);
            existing(restored);

            service.changeStatus(1L, new RequestStatusChangeDto(RequestStatus.PUBLISHED, null));

            assertThat(restored.getDeactivatedBy()).isNull();
        }

        @Test
        @DisplayName("the flag records the centre when the centre is the one withdrawing")
        void flagRecordsTheCentre() {
            Request live = request(1L, ownCenter, RequestStatus.PUBLISHED);
            existing(live);

            service.changeStatusByCenter(
                    1L, new RequestStatusChangeDto(RequestStatus.INACTIVE, "withdrawn by us"));

            assertThat(live.getDeactivatedBy()).isEqualTo(UserRole.CENTER);
        }

        /**
         * The two-step escape this guard used to allow.
         *
         * <p>{@code assertNotAdminTakedown} once exempted an {@code INACTIVE} target, on the
         * reasoning that deactivating something already deactivated changes nothing. It did: the
         * transition is an identity so the policy waved it through, and {@code applyStatus} then
         * wrote {@code deactivatedBy = CENTER} over the admin's flag and the centre's note over the
         * admin's mandatory reason. One more call to {@code /submit} and the request was live again.
         */
        @Test
        @DisplayName("a centre cannot overwrite an admin's takedown with one of its own")
        void centreCannotRelabelAnAdminTakedown() {
            Request takenDown = takenDownByAdmin(1L);
            existing(takenDown);

            assertThatThrownBy(() -> service.changeStatusByCenter(
                    1L, new RequestStatusChangeDto(RequestStatus.INACTIVE, "our own note")))
                    .as("re-deactivating an admin-deactivated request is refused outright")
                    .isInstanceOf(ForbiddenException.class);

            assertThat(takenDown.getDeactivatedBy())
                    .as("the admin's flag survives")
                    .isEqualTo(UserRole.ADMIN);
            assertThat(takenDown.getStatusNote())
                    .as("and so does the reason the admin had to give")
                    .isEqualTo("removed by an admin for a stated reason");
            assertThatThrownBy(() -> service.publishByCenter(1L))
                    .as("so the centre is still locked out afterwards")
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("COMPLETED is terminal for the centre that set it")
    class Completed {

        @Test
        @DisplayName("editing a completed request is a conflict, not a silent no-op")
        void notEditable() {
            existing(request(1L, ownCenter, RequestStatus.COMPLETED));

            assertThatThrownBy(() -> service.updateByCenter(1L, updateDto()))
                    .isInstanceOf(ConflictException.class)
                    .extracting(e -> ((ConflictException) e).getCode())
                    .isEqualTo("NOT_EDITABLE");
        }

        @Test
        @DisplayName("deleting a completed request is a conflict")
        void notDeletable() {
            Request completed = request(1L, ownCenter, RequestStatus.COMPLETED);
            existing(completed);

            assertThatThrownBy(() -> service.softDeleteByCenter(1L))
                    .isInstanceOf(ConflictException.class)
                    .extracting(e -> ((ConflictException) e).getCode())
                    .isEqualTo("NOT_DELETABLE");
            assertThat(completed.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("an admin can still take a completed request down")
        void adminMayTakeItDown() {
            Request completed = request(1L, ownCenter, RequestStatus.COMPLETED);
            existing(completed);

            service.changeStatus(1L, new RequestStatusChangeDto(RequestStatus.INACTIVE, "fraudulent"));

            assertThat(completed.getStatus()).isEqualTo(RequestStatus.INACTIVE);
            assertThat(completed.getDeactivatedBy()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("an admin can delete one, and deletion stays soft")
        void adminDeleteIsStillSoft() {
            Request completed = request(1L, ownCenter, RequestStatus.COMPLETED);
            existing(completed);

            service.softDeleteByAdmin(1L);

            assertThat(completed.getDeletedAt()).isNotNull();
            verify(requests, never()).delete(any(Request.class));
            verify(requests, never()).deleteById(any());
        }

        private RequestUpdateDto updateDto() {
            return new RequestUpdateDto("new title", 3L, "d", new BigDecimal("5000"),
                    null, null, null, null, null);
        }
    }

    @Nested
    @DisplayName("a removed request answers 410, not 404")
    class PublicResolution {

        @Test
        @DisplayName("a slug that never existed is 404")
        void unknownSlugIsNotFound() {
            when(requests.findBySlugAndDeletedAtIsNull("nope")).thenReturn(Optional.empty());
            when(requests.findBySlug("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.publicDetail("nope"))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("a soft-deleted slug is 410, which is the whole reason the row is kept")
        void deletedSlugIsGone() {
            Request deleted = request(1L, ownCenter, RequestStatus.PUBLISHED);
            deleted.setDeletedAt(LocalDateTime.now());
            when(requests.findBySlugAndDeletedAtIsNull(deleted.getSlug())).thenReturn(Optional.empty());
            when(requests.findBySlug(deleted.getSlug())).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> service.publicDetail(deleted.getSlug()))
                    .isInstanceOf(GoneException.class);
        }

        @Test
        @DisplayName("a draft is 404: it was never public, so there is nothing to have withdrawn")
        void draftIsNotFound() {
            Request draft = request(1L, ownCenter, RequestStatus.DRAFT);
            when(requests.findBySlugAndDeletedAtIsNull(draft.getSlug())).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> service.publicDetail(draft.getSlug()))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("a request an admin took down is 404 to the public, not 410")
        void inactiveIsNotFound() {
            Request down = takenDownByAdmin(1L);
            when(requests.findBySlugAndDeletedAtIsNull(down.getSlug())).thenReturn(Optional.of(down));

            assertThatThrownBy(() -> service.publicDetail(down.getSlug()))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("a completed request keeps a working URL")
        void completedStillResolves() {
            // Its URL is already indexed and linked from the messaging channels; 404ing it would be
            // self-inflicted.
            Request completed = request(1L, ownCenter, RequestStatus.COMPLETED);
            when(requests.findBySlugAndDeletedAtIsNull(completed.getSlug()))
                    .thenReturn(Optional.of(completed));
            when(requests.countByCenterIdAndDeletedAtIsNull(OWN_CENTER_ID)).thenReturn(3L);

            assertThatCode(() -> service.publicDetail(completed.getSlug())).doesNotThrowAnyException();
            verify(mapper).toDetail(completed, 3L, false);
        }

        @Test
        @DisplayName("the public detail never carries the private status note")
        void privateNotesStayPrivate() {
            Request completed = request(1L, ownCenter, RequestStatus.COMPLETED);
            when(requests.findBySlugAndDeletedAtIsNull(completed.getSlug()))
                    .thenReturn(Optional.of(completed));

            service.publicDetail(completed.getSlug());

            // The third argument is includePrivateNotes; false is the only correct value here.
            verify(mapper).toDetail(any(Request.class), anyLong(), eq(false));
        }
    }

    @Nested
    @DisplayName("a visitor may only ask for statuses that are public")
    class PublicStatusFilter {

        private final Pageable page = PageRequest.of(0, 20);

        @Test
        @DisplayName("asking for a non-public status is rejected outright")
        void inactiveIsRefused() {
            assertThatThrownBy(() -> service.publicList(filterWith(RequestStatus.INACTIVE), page))
                    .isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> service.publicList(filterWith(RequestStatus.DRAFT), page))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("a mixed request is filtered down rather than refused")
        void publicSubsetSurvives() {
            when(requests.findAll(ArgumentMatchers.<Specification<Request>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThatCode(() -> service.publicList(
                    filterWith(RequestStatus.DRAFT, RequestStatus.PUBLISHED), page))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("COMPLETED is requestable even though it is not shown by default")
        void completedIsRequestable() {
            when(requests.findAll(ArgumentMatchers.<Specification<Request>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThatCode(() -> service.publicList(filterWith(RequestStatus.COMPLETED), page))
                    .doesNotThrowAnyException();
        }

        private RequestFilter filterWith(RequestStatus... statuses) {
            return new RequestFilter(null, null, null, null, null, null, null, null,
                    List.of(statuses), null);
        }
    }

    @Nested
    @DisplayName("publication happens once")
    class Publication {

        @Test
        @DisplayName("first publication stamps publishedAt and announces")
        void firstPublicationAnnounces() {
            Request draft = request(1L, ownCenter, RequestStatus.DRAFT);
            existing(draft);

            service.publishByCenter(1L);

            assertThat(draft.getPublishedAt()).isNotNull();
            verify(events).publishEvent(any(RequestPublishedEvent.class));
        }

        @Test
        @DisplayName("restoring a withdrawn request does not announce it again")
        void restoringDoesNotReannounce() {
            // Otherwise every takedown-and-restore would re-post the request to Telegram and Bale.
            Request withdrawn = request(1L, ownCenter, RequestStatus.INACTIVE);
            withdrawn.setDeactivatedBy(UserRole.CENTER);
            withdrawn.setPublishedAt(LocalDateTime.now().minusDays(3));
            existing(withdrawn);

            service.publishByCenter(1L);

            verify(events, never()).publishEvent(any(RequestPublishedEvent.class));
        }

        @Test
        @DisplayName("the slug is frozen once the request has been public")
        void slugFrozenAfterPublication() {
            // Changing a live URL without a redirect discards whatever ranking it has earned.
            Request live = request(1L, ownCenter, RequestStatus.PUBLISHED);
            live.setPublishedAt(LocalDateTime.now().minusDays(1));
            String slugBefore = live.getSlug();
            existing(live);
            grantCategory(3L);

            service.updateByCenter(1L, new RequestUpdateDto("a completely different title", 3L,
                    "d", new BigDecimal("5000"), null, null, null, null, null));

            assertThat(live.getTitle()).isEqualTo("a completely different title");
            assertThat(live.getSlug()).isEqualTo(slugBefore);
        }

        @Test
        @DisplayName("a draft's slug still tracks its title")
        void draftSlugFollowsTheTitle() {
            Request draft = request(1L, ownCenter, RequestStatus.DRAFT);
            existing(draft);
            grantCategory(3L);

            service.updateByCenter(1L, new RequestUpdateDto("renamed", 3L,
                    "d", new BigDecimal("5000"), null, null, null, null, null));

            assertThat(draft.getSlug()).isEqualTo("renamed-rq-1001");
        }
    }

    @Nested
    @DisplayName("a centre may only publish in the categories it was granted")
    class CategoryGrant {

        @Test
        @DisplayName("an ungranted category is refused")
        void ungrantedIsRefused() {
            Request draft = request(1L, ownCenter, RequestStatus.DRAFT);
            existing(draft);
            when(categories.findById(3L)).thenReturn(Optional.of(category(3L)));
            // ownCenter's category set is left empty.

            assertThatThrownBy(() -> service.updateByCenter(1L, new RequestUpdateDto("t", 3L,
                    "d", new BigDecimal("5000"), null, null, null, null, null)))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("a category that does not exist is a 404, not a validation error")
        void missingCategoryIsNotFound() {
            Request draft = request(1L, ownCenter, RequestStatus.DRAFT);
            existing(draft);
            when(categories.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateByCenter(1L, new RequestUpdateDto("t", 99L,
                    "d", new BigDecimal("5000"), null, null, null, null, null)))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("announcing to a channel")
    class Announce {

        @Test
        @DisplayName("only a published request can be announced")
        void onlyPublished() {
            for (RequestStatus status : List.of(RequestStatus.DRAFT, RequestStatus.COMPLETED,
                    RequestStatus.INACTIVE)) {
                Request request = request(1L, ownCenter, status);
                when(requests.findForMessaging(1L)).thenReturn(Optional.of(request));

                assertThatThrownBy(() -> service.loadForAnnounce(1L, true))
                        .as("status %s", status)
                        .isInstanceOf(ConflictException.class)
                        .extracting(e -> ((ConflictException) e).getCode())
                        .isEqualTo("NOT_PUBLISHED");
            }
        }

        @Test
        @DisplayName("a centre cannot announce another centre's request")
        void ownershipIsCheckedForCentres() {
            Request foreign = request(1L, center(OTHER_CENTER_ID), RequestStatus.PUBLISHED);
            when(requests.findForMessaging(1L)).thenReturn(Optional.of(foreign));

            assertThatThrownBy(() -> service.loadForAnnounce(1L, true))
                    .isInstanceOf(ForbiddenException.class);
            assertThatCode(() -> service.loadForAnnounce(1L, false)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a soft-deleted request cannot be announced")
        void deletedCannotBeAnnounced() {
            Request deleted = request(1L, ownCenter, RequestStatus.PUBLISHED);
            deleted.setDeletedAt(LocalDateTime.now());
            when(requests.findForMessaging(1L)).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> service.loadForAnnounce(1L, true))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("soft delete")
    class SoftDelete {

        @Test
        @DisplayName("deleting twice does not move the timestamp")
        void isIdempotent() {
            Request request = request(1L, ownCenter, RequestStatus.PUBLISHED);
            existing(request);

            service.softDeleteByAdmin(1L);
            LocalDateTime first = request.getDeletedAt();
            // The second call must find it already deleted and return without touching the row.
            assertThatThrownBy(() -> service.softDeleteByAdmin(1L))
                    .isInstanceOf(NotFoundException.class);

            assertThat(request.getDeletedAt()).isEqualTo(first);
        }
    }

    private void grantCategory(long id) {
        Category category = category(id);
        Set<Category> granted = new HashSet<>();
        granted.add(category);
        ownCenter.setCategories(granted);
        when(categories.findById(id)).thenReturn(Optional.of(category));
    }

    private static Category category(long id) {
        return Category.builder().id(id).build();
    }
}
