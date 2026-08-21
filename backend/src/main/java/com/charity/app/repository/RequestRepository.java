package com.charity.app.repository;

import com.charity.app.model.Request;
import com.charity.app.model.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Replaces the nine derived finders and six near-identical {@code CASE WHEN} queries the old
 * {@code CharityCaseRepository} carried. All filtering now goes through
 * {@link com.charity.app.repository.spec.RequestSpecifications}, and urgency ordering is a plain
 * sort on {@code urgencyRank}.
 */
@Repository
public interface RequestRepository extends JpaRepository<Request, Long>, JpaSpecificationExecutor<Request> {

    /**
     * Every listing renders the centre, its city and the category of each row, which was previously
     * an N+1. All the paths are to-one, so Hibernate can still do the paging in SQL -- do not be
     * tempted to add {@code center.categories} here, a to-many would force it to page in memory.
     *
     * <p>The city is reached through the centre; a request has none of its own.
     */
    @Override
    @EntityGraph(attributePaths = {
            "center", "center.city", "center.city.province", "center.province", "category"})
    Page<Request> findAll(Specification<Request> spec, Pageable pageable);

    /**
     * Loads a request with everything the bot message template dereferences.
     *
     * <p>The announcement listener is {@code @Async}, so it runs on a thread with no persistence
     * context, and {@code open-in-view} is off. A plain {@code findById} therefore hands it a
     * detached entity whose associations are uninitialised proxies, and the first
     * {@code getCategory().getName()} throws {@code LazyInitializationException} -- straight into
     * the catch-all in {@code AbstractBotMessagingService}, which logs and returns null. The
     * result is that nothing is ever posted and {@code balePosted} stays false, with no HTTP call
     * attempted. That is why no request had ever reached Bale.
     *
     * <p>Written as JPQL rather than {@code @EntityGraph} on purpose. Entity-graph attribute paths
     * are strings that are not checked until the query runs, and a typo there has already cost this
     * project a production 500. A bad path in this query fails at context startup instead.
     *
     * <p>{@code r.documents} and their categories are fetched for the same reason: the template
     * sends one attachment per document and captions it with its category name. Add a field to the
     * template, add it here.
     *
     * <p>{@code DISTINCT} is not decoration. Fetch-joining a collection multiplies the result rows,
     * and this method returns an {@code Optional} -- without it, the first request carrying two
     * documents would make Spring Data throw {@code NonUniqueResultException} and take every
     * announcement down with it.
     */
    @Query("""
           SELECT DISTINCT r FROM Request r
           LEFT JOIN FETCH r.center c
           LEFT JOIN FETCH c.city
           LEFT JOIN FETCH r.category
           LEFT JOIN FETCH r.documents d
           LEFT JOIN FETCH d.category
           WHERE r.id = :id
           """)
    Optional<Request> findForMessaging(@Param("id") Long id);

    Optional<Request> findBySlugAndDeletedAtIsNull(String slug);

    /** Used to tell "soft-deleted" (410) apart from "never existed" (404). */
    Optional<Request> findBySlug(String slug);

    Optional<Request> findByCodeIgnoreCase(String code);

    boolean existsBySlug(String slug);

    long countByCategoryIdAndDeletedAtIsNull(Long categoryId);

    long countByCenterIdAndDeletedAtIsNull(Long centerId);

    /** Per-status totals for the admin stat cards. Statuses with no rows are simply absent. */
    @Query("SELECT r.status AS status, COUNT(r) AS total FROM Request r WHERE r.deletedAt IS NULL GROUP BY r.status")
    List<StatusCount> countGroupedByStatus();

    @Query("""
           SELECT r.status AS status, COUNT(r) AS total FROM Request r
           WHERE r.deletedAt IS NULL AND r.center.id = :centerId
           GROUP BY r.status
           """)
    List<StatusCount> countGroupedByStatusForCenter(@Param("centerId") Long centerId);

    /** One query for a whole page of centres rather than one per centre. */
    @Query("""
           SELECT r.center.id AS centerId, COUNT(r) AS total FROM Request r
           WHERE r.deletedAt IS NULL AND r.status = :status AND r.center.id IN :centerIds
           GROUP BY r.center.id
           """)
    List<CenterCount> countActiveByCenterIds(@Param("status") RequestStatus status,
                                             @Param("centerIds") Collection<Long> centerIds);

    @Query("""
           SELECT r.category.id AS categoryId, COUNT(r) AS total FROM Request r
           WHERE r.deletedAt IS NULL AND r.status = :status
           GROUP BY r.category.id
           """)
    List<CategoryCount> countActiveByCategory(@Param("status") RequestStatus status);

    /** Bulk reassignment used when an admin deletes a category and picks a replacement. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Request r SET r.category.id = :replacementId WHERE r.category.id = :categoryId")
    int reassignCategory(@Param("categoryId") Long categoryId, @Param("replacementId") Long replacementId);

    /** Feeds the sitemap. Only rows that are actually indexable. */
    @Query("""
           SELECT r FROM Request r
           WHERE r.deletedAt IS NULL AND r.status IN :statuses
           ORDER BY r.updatedAt DESC
           """)
    Page<Request> findIndexable(@Param("statuses") Collection<RequestStatus> statuses, Pageable pageable);

    interface StatusCount {
        RequestStatus getStatus();

        long getTotal();
    }

    interface CenterCount {
        Long getCenterId();

        long getTotal();
    }

    interface CategoryCount {
        Long getCategoryId();

        long getTotal();
    }
}
