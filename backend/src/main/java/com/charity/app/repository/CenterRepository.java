package com.charity.app.repository;

import com.charity.app.model.Center;
import com.charity.app.model.enums.CenterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CenterRepository extends JpaRepository<Center, Long> {

    Optional<Center> findByUserId(Long userId);

    Optional<Center> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    /*
     * The two paged listings below fetch to-one associations only, and that restriction is the
     * whole point rather than an oversight.
     *
     * Both graphs used to name `categories`, which is a collection. A collection fetch join
     * multiplies the result rows, so Hibernate cannot express `LIMIT`/`OFFSET` in SQL and instead
     * logs HHH90003004 and paginates in memory -- it read *every* centre row on every request and
     * threw away all but a page of them. The listing looked correct throughout, and got linearly
     * slower with each centre an admin created.
     *
     * `categories` and `documents` are now loaded by {@code @BatchSize} on the entity instead: one
     * extra statement for the whole page rather than a join that breaks paging, and rather than the
     * per-row queries that dropping the fetch on its own would have left behind. See Center.
     *
     * Nested to-one paths are named explicitly. `city.province` is read by CenterMapper through
     * CityRef and was a lazy load per distinct city; `user` is read only by the admin response, for
     * the username and email, so the public query does not join it.
     */

    @EntityGraph(attributePaths = {"city", "city.province", "province"})
    Page<Center> findByStatus(CenterStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"city", "city.province", "province", "user"})
    Page<Center> findAllBy(Pageable pageable);

    @Query("SELECT c FROM Center c WHERE c.status = :status ORDER BY c.updatedAt DESC")
    Page<Center> findIndexable(@Param("status") CenterStatus status, Pageable pageable);

    long countByCategories_Id(Long categoryId);

    long countByCityId(Long cityId);

    long countByProvinceId(Long provinceId);

    /**
     * Detaches a category from every centre. Needed before deleting a category, because the
     * center_categories rows would otherwise fail the foreign key -- which previously surfaced as a
     * raw DataIntegrityViolationException and a 500 with SQL text in the body.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM center_categories WHERE category_id = :categoryId", nativeQuery = true)
    void detachCategoryFromAllCenters(@Param("categoryId") Long categoryId);

    /**
     * Grants the replacement category to every centre that held the one being deleted, so the
     * reassigned requests stay consistent with their centre's allowed set.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
           INSERT INTO center_categories (center_id, category_id)
           SELECT cc.center_id, :replacementId FROM center_categories cc
           WHERE cc.category_id = :categoryId
             AND NOT EXISTS (SELECT 1 FROM center_categories x
                             WHERE x.center_id = cc.center_id AND x.category_id = :replacementId)
           """, nativeQuery = true)
    void grantReplacementCategory(@Param("categoryId") Long categoryId,
                                  @Param("replacementId") Long replacementId);

    List<Center> findByStatusOrderByNameAsc(CenterStatus status);
}
