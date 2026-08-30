package com.charity.app.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The authorisation boundary as the filter chain actually applies it.
 *
 * <p>Three rules, and each has failed here before in a way reading the config would not reveal.
 * {@code /api/admin/**} is admin-only and {@code /api/center/**} is centre-only, so the two panels
 * cannot reach into each other. {@code /api/public/**} is open for {@code GET} <b>and only for
 * GET</b> -- the matcher carries an explicit method, and dropping it would expose every write path
 * that ever gets added under that prefix. And an unauthenticated call must answer 401 rather than
 * a redirect to a login form that does not exist in a stateless API.
 *
 * <p>Denials are asserted by status code rather than by reading {@link SecurityConfig}, because the
 * thing that goes wrong is never the rule as written -- it is a rule that stops being reached:
 * {@code @PreAuthorize} raising an exception the error handler turns into a 500, an entry point
 * that answers 302, a matcher shadowed by an earlier one.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@DisplayName("the API authorisation boundary")
class ApiAuthorizationTest {

    @Autowired private MockMvc mvc;

    @Nested
    @DisplayName("an anonymous caller")
    class Anonymous {

        @Test
        @WithAnonymousUser
        @DisplayName("may read the public site")
        void mayReadPublicEndpoints() throws Exception {
            mvc.perform(get("/api/public/requests")).andExpect(status().isOk());
            mvc.perform(get("/api/public/categories")).andExpect(status().isOk());
            mvc.perform(get("/api/public/centers")).andExpect(status().isOk());
            mvc.perform(get("/api/public/provinces")).andExpect(status().isOk());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("is refused both panels with 401, not a redirect")
        void isRefusedBothPanels() throws Exception {
            // A 302 to a login page would be the default and is wrong for a stateless JSON API --
            // the frontend would follow it and get HTML where it expected an error body.
            mvc.perform(get("/api/admin/requests")).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/center/requests")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("cannot write under the public prefix")
        void cannotWriteUnderPublic() throws Exception {
            // permitAll is scoped to GET. If that method restriction is ever dropped, this is what
            // notices -- before some future POST under /api/public becomes an open write endpoint.
            mvc.perform(post("/api/public/requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * Spring raises {@code NoResourceFoundException} for a URL that matches no handler and no
         * static resource. Nothing mapped it, so it fell through to the catch-all
         * {@code @ExceptionHandler(Exception.class)}: every mistyped path and every stale crawler
         * hit answered {@code INTERNAL_ERROR} and was logged at ERROR with a trace id. Nothing was
         * exposed by it, but it cost the error log its meaning -- and a 500 tells a crawler to come
         * back later rather than to drop the link.
         */
        @Test
        @WithAnonymousUser
        @DisplayName("gets 404, not a server error, for an unknown path")
        void unknownPublicPathIsNotFound() throws Exception {
            mvc.perform(get("/api/public/no-such-thing")).andExpect(status().isNotFound());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("the 404 body is the ordinary one, not the internal-error shape")
        void unknownPathCarriesTheNormalNotFoundBody() throws Exception {
            // A traceId in the body would mean it is still going through the catch-all.
            mvc.perform(get("/api/public/no-such-thing"))
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.traceId").doesNotExist());
        }
    }

    @Nested
    @DisplayName("a centre account")
    class CentreAccount {

        @Test
        @WithMockUser(username = "centre-1", roles = "CENTER")
        @DisplayName("cannot reach the admin panel, and is told so with 403")
        void cannotReachAdmin() throws Exception {
            // 403 rather than 500: @PreAuthorize's AuthorizationDeniedException used to fall through
            // to the catch-all handler and surface as a server error.
            mvc.perform(get("/api/admin/requests")).andExpect(status().isForbidden());
            mvc.perform(get("/api/admin/centers")).andExpect(status().isForbidden());
            mvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "centre-1", roles = "CENTER")
        @DisplayName("reaches its own panel, where the failure is about data rather than access")
        void reachesItsOwnPanel() throws Exception {
            // No centre row is attached to this username, so the service answers 404. What matters
            // is that the request got past the filter chain at all -- anything in the 4xx range
            // other than 401 or 403 proves the routing rule matched.
            mvc.perform(get("/api/center/requests")).andExpect(result ->
                    assertThat(result.getResponse().getStatus())
                            .as("the request must get past the filter chain")
                            .isNotIn(401, 403));
        }
    }

    @Nested
    @DisplayName("an admin account")
    class AdminAccount {

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        @DisplayName("reaches the admin panel")
        void reachesAdmin() throws Exception {
            mvc.perform(get("/api/admin/requests")).andExpect(status().isOk());
            mvc.perform(get("/api/admin/requests/stats")).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        @DisplayName("cannot reach the centre panel: the roles are exclusive, not nested")
        void cannotReachCentre() throws Exception {
            // ADMIN is not a superset of CENTER here. An admin acting on a centre's behalf goes
            // through /api/admin, which records that it was an admin who acted.
            mvc.perform(get("/api/center/requests")).andExpect(status().isForbidden());
        }
    }
}
