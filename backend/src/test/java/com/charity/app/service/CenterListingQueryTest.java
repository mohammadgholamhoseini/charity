package com.charity.app.service;

import com.charity.app.common.Paging;
import com.charity.app.model.Category;
import com.charity.app.model.Center;
import com.charity.app.model.CenterDocument;
import com.charity.app.model.DocumentCategory;
import com.charity.app.model.User;
import com.charity.app.model.enums.CenterStatus;
import com.charity.app.model.enums.DocumentScope;
import com.charity.app.model.enums.UserRole;
import com.charity.app.payload.CenterResponse;
import com.charity.app.repository.CategoryRepository;
import com.charity.app.repository.CenterRepository;
import com.charity.app.repository.DocumentCategoryRepository;
import com.charity.app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the centre listings against the two ways they have degraded quietly.
 *
 * <p>Both are invisible from the outside. The listing renders correctly either way; it just gets
 * linearly slower with every centre an admin creates, and nothing in the response says so. The
 * only signal was one WARN line in the startup log that nobody was reading:
 *
 * <pre>HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory</pre>
 *
 * <p>That is Hibernate saying the query it built cannot be paged in SQL, so it read every row and
 * discarded all but a page. It was caused by naming a collection in the {@code @EntityGraph}: a
 * collection fetch join multiplies the result rows, and {@code LIMIT} would then cut a centre's
 * categories in half rather than cut the list of centres. Dropping the fetch on its own would have
 * traded it for a query per centre, so the collections are batch-loaded instead.
 *
 * <p>Asserted through Hibernate's own statistics rather than by reading the SQL, because the two
 * things worth pinning are exactly the two numbers it reports: <b>how many centre rows were
 * loaded</b> (a page's worth, not the table) and <b>how many statements it took</b> (a constant,
 * not one per row). A test that grew the data and watched the time would be flaky; these numbers
 * are exact.
 *
 * <p>Statistics are switched on for this class only, which gives it a context of its own -- the one
 * cost of the approach, and worth it for the two regressions it makes impossible to reintroduce.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("local")
@Transactional
@DisplayName("the centre listings page in SQL and load their associations in bulk")
class CenterListingQueryTest {

    /** Comfortably more than one page, so in-memory pagination cannot hide behind a small table. */
    private static final int CENTRES = 30;
    private static final int PAGE_SIZE = 5;

    /**
     * A page query, a count query, and one batched statement per lazy association. The exact figure
     * is not the contract -- "a constant that does not grow with the page" is. The bound is set well
     * above what the fix needs and still far below one-query-per-row.
     */
    private static final int STATEMENT_BUDGET = 12;

    @Autowired private CenterService centerService;
    @Autowired private CenterRepository centers;
    @Autowired private CategoryRepository categories;
    @Autowired private DocumentCategoryRepository documentCategories;
    @Autowired private UserRepository users;
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void seedCentres() {
        Category category = categories.findAll().stream().findFirst().orElseThrow();
        DocumentCategory documentCategory = documentCategories
                .findByScopeAndActiveTrueOrderBySortOrderAscNameAsc(DocumentScope.CENTER)
                .getFirst();

        for (int i = 0; i < CENTRES; i++) {
            User user = users.save(User.builder()
                    .username("listing-centre-" + i)
                    .email("listing-centre-" + i + "@test.local")
                    .password("irrelevant")
                    .role(UserRole.CENTER)
                    .enabled(true)
                    .build());

            Center center = Center.builder()
                    .user(user)
                    .name("centre " + i)
                    .slug("listing-centre-" + i)
                    .status(CenterStatus.APPROVED)
                    .categories(new HashSet<>(List.of(category)))
                    .build();
            // Documents are rendered by the admin response, so a centre with none would not
            // exercise the association this is meant to guard.
            center.getDocuments().add(CenterDocument.builder()
                    .center(center)
                    .category(documentCategory)
                    .storedFilename("licence-" + i + ".pdf")
                    .originalFilename("licence.pdf")
                    .title("licence")
                    .sortOrder(0)
                    .build());
            centers.save(center);
        }

        // Everything above must be in the database, and out of the first-level cache, before a
        // single statement is counted -- otherwise the listing would read its entities from the
        // persistence context and every count below would be zero.
        em.flush();
        em.clear();

        statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.clear();
    }

    private long centresLoaded() {
        return statistics.getEntityStatistics(Center.class.getName()).getLoadCount();
    }

    @Test
    @DisplayName("the admin listing reads one page of centres, not the whole table")
    void adminListingPagesInSql() {
        Page<CenterResponse> page = centerService.adminList(
                PageRequest.of(0, PAGE_SIZE, Sort.by("name")));

        assertThat(page.getContent()).hasSize(PAGE_SIZE);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(CENTRES);
        assertThat(centresLoaded())
                .as("HHH90003004: a collection in the entity graph would load all %d rows here", CENTRES)
                .isEqualTo(PAGE_SIZE);
    }

    @Test
    @DisplayName("the admin listing costs a constant number of statements, not one per centre")
    void adminListingDoesNotIssueAQueryPerRow() {
        centerService.adminList(PageRequest.of(0, PAGE_SIZE, Sort.by("name")));

        assertThat(statistics.getPrepareStatementCount())
                .as("categories, documents and user are each one batched statement for the page")
                .isLessThanOrEqualTo(STATEMENT_BUDGET);
    }

    @Test
    @DisplayName("the admin listing renders the associations it pages away from")
    void adminListingStillCarriesItsAssociations() {
        // The cheap way to make the assertions above pass is to stop loading the data. This is what
        // stops that: the batched associations must still reach the response.
        CenterResponse response = centerService
                .adminList(PageRequest.of(0, PAGE_SIZE, Sort.by("name")))
                .getContent()
                .getFirst();

        assertThat(response.categories()).isNotEmpty();
        assertThat(response.documents()).isNotEmpty();
        assertThat(response.username()).isNotBlank();
    }

    @Test
    @DisplayName("the public listing pages in SQL too")
    void publicListingPagesInSql() {
        // Same entity graph, same defect, and this is the one visitors hit.
        centerService.publicList(PageRequest.of(0, PAGE_SIZE, Sort.by("name")));

        assertThat(centresLoaded()).isEqualTo(PAGE_SIZE);
    }

    @Test
    @DisplayName("the public listing carries each centre's categories")
    void publicListingStillCarriesCategories() {
        var cards = centerService.publicList(PageRequest.of(0, PAGE_SIZE, Sort.by("name"))).getContent();

        assertThat(cards).allSatisfy(card -> assertThat(card.categories()).isNotEmpty());
    }

    @Test
    @DisplayName("a full page is still one batch, so the batch size covers the largest page served")
    void batchSizeCoversTheLargestPage() {
        // Paging.MAX_SIZE is what the API will serve and what @BatchSize is set to. If someone
        // raises one without the other, the largest page starts costing several round trips.
        assertThat(Paging.MAX_SIZE).isGreaterThanOrEqualTo(Paging.DEFAULT_SIZE);

        centerService.adminList(PageRequest.of(0, Paging.MAX_SIZE, Sort.by("name")));

        assertThat(statistics.getPrepareStatementCount())
                .as("a %d-row page must not cost more statements than a %d-row one",
                        Paging.MAX_SIZE, PAGE_SIZE)
                .isLessThanOrEqualTo(STATEMENT_BUDGET);
    }
}
