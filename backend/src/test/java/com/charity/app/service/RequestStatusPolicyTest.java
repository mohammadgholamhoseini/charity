package com.charity.app.service;

import com.charity.app.common.error.ConflictException;
import com.charity.app.common.error.ValidationException;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.charity.app.model.enums.RequestStatus.COMPLETED;
import static com.charity.app.model.enums.RequestStatus.DRAFT;
import static com.charity.app.model.enums.RequestStatus.INACTIVE;
import static com.charity.app.model.enums.RequestStatus.PENDING;
import static com.charity.app.model.enums.RequestStatus.PUBLISHED;
import static com.charity.app.model.enums.RequestStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The transition matrix, asserted over its whole surface rather than at the handful of points some
 * feature happened to exercise.
 *
 * <p>The expected sets below are deliberately <b>re-stated</b> from the domain rules instead of
 * being read off {@link RequestStatusPolicy}'s own maps. A test that imports the table it is
 * checking passes whatever the table says; this one fails when the table changes, which is the
 * point. The two rules it guards -- COMPLETED is terminal, and the admin's COMPLETED -> INACTIVE
 * is the single exception -- are exactly the ones that do quiet harm when widened by accident.
 *
 * <p>Every ordered pair of statuses is tried in both roles, so a widening anywhere breaks a test,
 * including for the two deprecated values nothing produces any more.
 */
class RequestStatusPolicyTest {

    private final RequestStatusPolicy policy = new RequestStatusPolicy();

    /** What an admin may do. Transcribed from the rules, not from the implementation. */
    private static final Map<RequestStatus, Set<RequestStatus>> ADMIN_EXPECTED = Map.of(
            DRAFT, EnumSet.of(PUBLISHED, INACTIVE),
            PENDING, EnumSet.noneOf(RequestStatus.class),
            PUBLISHED, EnumSet.of(COMPLETED, INACTIVE),
            REJECTED, EnumSet.noneOf(RequestStatus.class),
            COMPLETED, EnumSet.of(INACTIVE),
            INACTIVE, EnumSet.of(PUBLISHED));

    /** What a centre may do: the same, minus any way out of COMPLETED. */
    private static final Map<RequestStatus, Set<RequestStatus>> CENTER_EXPECTED = Map.of(
            DRAFT, EnumSet.of(PUBLISHED, INACTIVE),
            PENDING, EnumSet.noneOf(RequestStatus.class),
            PUBLISHED, EnumSet.of(COMPLETED, INACTIVE),
            REJECTED, EnumSet.noneOf(RequestStatus.class),
            COMPLETED, EnumSet.noneOf(RequestStatus.class),
            INACTIVE, EnumSet.of(PUBLISHED));

    private static Stream<Arguments> everyTransition() {
        return Stream.of(UserRole.values()).flatMap(actor ->
                Stream.of(RequestStatus.values()).flatMap(from ->
                        Stream.of(RequestStatus.values())
                                .map(to -> Arguments.of(actor, from, to))));
    }

    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @MethodSource("everyTransition")
    @DisplayName("the matrix permits exactly the transitions the rules describe, and no others")
    void matrixMatchesTheRules(UserRole actor, RequestStatus from, RequestStatus to) {
        boolean expectedAllowed = from == to || expectedFor(actor).get(from).contains(to);

        if (expectedAllowed) {
            assertThatCode(() -> policy.assertTransition(from, to, actor)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> policy.assertTransition(from, to, actor))
                    .isInstanceOf(ConflictException.class)
                    .extracting(e -> ((ConflictException) e).getCode())
                    .isEqualTo("INVALID_TRANSITION");
        }
    }

    private static Map<RequestStatus, Set<RequestStatus>> expectedFor(UserRole actor) {
        return actor == UserRole.CENTER ? CENTER_EXPECTED : ADMIN_EXPECTED;
    }

    /**
     * The policy picks its table with {@code actor == CENTER ? CENTER_ALLOWED : ADMIN_ALLOWED}, so
     * a third role would silently inherit full admin powers. This is what notices.
     */
    @Test
    @DisplayName("UserRole still has exactly the two roles the matrix distinguishes")
    void onlyTwoRolesExist() {
        assertThat(UserRole.values())
                .as("a third role would fall through to the admin matrix by default")
                .containsExactlyInAnyOrder(UserRole.ADMIN, UserRole.CENTER);
    }

    @Nested
    @DisplayName("COMPLETED is terminal")
    class CompletedIsTerminal {

        @ParameterizedTest
        @EnumSource(RequestStatus.class)
        @DisplayName("a centre cannot move a completed request anywhere at all")
        void centreCannotLeaveCompleted(RequestStatus target) {
            if (target == COMPLETED) {
                assertThatCode(() -> policy.assertTransition(COMPLETED, COMPLETED, UserRole.CENTER))
                        .doesNotThrowAnyException();
                return;
            }
            assertThatThrownBy(() -> policy.assertTransition(COMPLETED, target, UserRole.CENTER))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("an admin's only way out of COMPLETED is a takedown, not a reopening")
        void adminMayOnlyTakeDown() {
            assertThatCode(() -> policy.assertTransition(COMPLETED, INACTIVE, UserRole.ADMIN))
                    .doesNotThrowAnyException();
            assertThatThrownBy(() -> policy.assertTransition(COMPLETED, PUBLISHED, UserRole.ADMIN))
                    .isInstanceOf(ConflictException.class);
            assertThatThrownBy(() -> policy.assertTransition(COMPLETED, DRAFT, UserRole.ADMIN))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("a completed request is neither editable nor deletable by its centre")
        void completedIsFrozenForItsCentre() {
            assertThat(policy.isEditableByCenter(COMPLETED)).isFalse();
            assertThat(policy.isDeletableByCenter(COMPLETED)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = RequestStatus.class, names = "COMPLETED", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("every other status stays editable and deletable")
        void everythingElseIsOpen(RequestStatus status) {
            assertThat(policy.isEditableByCenter(status)).isTrue();
            assertThat(policy.isDeletableByCenter(status)).isTrue();
        }
    }

    @Nested
    @DisplayName("a takedown must carry a reason")
    class NoteRequirement {

        @ParameterizedTest
        @EnumSource(value = RequestStatus.class, names = "INACTIVE", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("no other target status requires a note")
        void noteOptionalElsewhere(RequestStatus target) {
            assertThatCode(() -> policy.assertNoteProvided(target, null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("INACTIVE rejects a missing, empty or whitespace-only note")
        void inactiveDemandsRealText() {
            for (String note : new String[]{null, "", "   ", "\t\n"}) {
                assertThatThrownBy(() -> policy.assertNoteProvided(INACTIVE, note))
                        .as("note: %s", note == null ? "null" : "[" + note + "]")
                        .isInstanceOf(ValidationException.class);
            }
        }

        @Test
        @DisplayName("INACTIVE accepts a note with content")
        void inactiveAcceptsAReason() {
            assertThatCode(() -> policy.assertNoteProvided(INACTIVE, "duplicate listing"))
                    .doesNotThrowAnyException();
        }
    }
}
