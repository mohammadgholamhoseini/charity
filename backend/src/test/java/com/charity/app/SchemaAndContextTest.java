package com.charity.app;

import com.charity.app.repository.CategoryRepository;
import com.charity.app.repository.CityRepository;
import com.charity.app.repository.ProvinceRepository;
import com.charity.app.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The gate this project has always had, run automatically instead of by eye.
 *
 * <p>{@code AGENTS.md} names one backend check: start the app on the {@code local} profile and see
 * that it comes up. That works because {@code local} builds an empty H2 database from the real
 * Flyway migrations and then asks Hibernate to {@code validate} the entities against the result --
 * so a migration that was never written, or an entity field with no column behind it, stops the
 * application rather than reaching production. This class is that same check, on the same profile,
 * in a form CI could run.
 *
 * <p>It is slower than everything else in the suite by an order of magnitude, and it is worth it:
 * a drifted migration is the one class of bug here that cannot be caught by reading the diff.
 */
@SpringBootTest
@ActiveProfiles("local")
@DisplayName("the migrations, the entities and the seed data agree")
class SchemaAndContextTest {

    @Autowired private Environment environment;
    @Autowired private Flyway flyway;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private ProvinceRepository provinces;
    @Autowired private CityRepository cities;
    @Autowired private CategoryRepository categories;
    @Autowired private UserRepository users;

    /**
     * Without this the whole class is theatre: if someone relaxes the profile to {@code none}, the
     * context would still start and every other assertion here would still pass, while the drift
     * this exists to catch would sail through.
     */
    @Test
    @DisplayName("the profile under test still validates entities against the built schema")
    void validationIsActuallyOn() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .as("the local profile must keep ddl-auto: validate, or this test proves nothing")
                .isEqualTo("validate");
    }

    @Test
    @DisplayName("every migration on disk has been applied, in order and unmodified")
    void migrationsAllApplied() throws IOException {
        List<MigrationInfo> applied = Arrays.stream(flyway.info().all())
                .filter(info -> info.getState() != null && info.getState().isApplied())
                .toList();

        assertThat(applied)
                .as("no migration may be pending, ignored or out of order")
                .hasSize(migrationFilesOnDisk());

        assertThat(Arrays.stream(flyway.info().all()).map(MigrationInfo::getState))
                .as("a FAILED or OUT_OF_ORDER state means the schema is not what the files describe")
                .doesNotContain(MigrationState.FAILED, MigrationState.OUT_OF_ORDER,
                        MigrationState.PENDING, MigrationState.IGNORED);
    }

    /**
     * Counted from the source tree rather than from Flyway's own history, so a migration file that
     * is added but never picked up -- a typo in the name, the wrong directory -- is a failure here
     * instead of a silent no-op.
     */
    private static int migrationFilesOnDisk() throws IOException {
        Path dir = Path.of("src/main/resources/db/migration");
        assertThat(dir).as("the migration directory moved").exists();
        try (Stream<Path> files = Files.list(dir)) {
            return (int) files.filter(p -> p.getFileName().toString().matches("V\\d+__.*\\.sql")).count();
        }
    }

    @Test
    @DisplayName("every entity Hibernate knows about is backed by a real table")
    void everyEntityIsMapped() {
        // ddl-auto: validate has already enforced this by the time the context is up -- the context
        // would not have started otherwise. Asserting the metamodel is non-empty is what stops that
        // guarantee from quietly becoming vacuous if entity scanning ever breaks.
        var entities = entityManagerFactory.getMetamodel().getEntities();

        assertThat(entities).as("no entities were scanned at all").isNotEmpty();
        assertThat(entities.stream().map(EntityType::getName))
                .contains("Request", "Center", "User", "Category", "City", "Province");
    }

    @Test
    @DisplayName("startup seeds the reference data an empty database needs to be usable")
    void referenceDataIsSeeded() {
        // Before these seeders existed an admin had to type every province and city by hand before
        // a single centre could be created, and the public city filter had nothing to filter on.
        assertThat(provinces.count()).as("provinces").isEqualTo(31);
        assertThat(cities.count()).as("cities").isGreaterThan(100);
        assertThat(categories.count()).as("categories").isPositive();
    }

    @Test
    @DisplayName("the initial admin exists and there is no second account beside it")
    void initialAdminIsSeeded() {
        // There is no public registration; every other account is created by this one.
        assertThat(users.count())
                .as("a fresh database should hold exactly the seeded admin")
                .isEqualTo(1);
    }
}
