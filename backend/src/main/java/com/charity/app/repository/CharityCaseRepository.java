package com.charity.app.repository;

import com.charity.app.model.CharityCase;
import com.charity.app.model.CharityCase.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharityCaseRepository extends JpaRepository<CharityCase, Long> {

    Page<CharityCase> findByStatusOrderByCreatedAtDesc(Status status, Pageable pageable);

    Page<CharityCase> findByCenterIdOrderByCreatedAtDesc(Long centerId, Pageable pageable);

    Page<CharityCase> findByCategoryIdAndStatusOrderByCreatedAtDesc(Long categoryId, Status status, Pageable pageable);

    Page<CharityCase> findByCenterId(Long centerId, Pageable pageable);

    List<CharityCase> findByStatus(Status status);

    List<CharityCase> findByCenterIdAndStatusNot(Long centerId, Status status);

    @Query("""
           SELECT c FROM CharityCase c
           WHERE c.status IN :statuses
           ORDER BY CASE c.urgency
               WHEN com.charity.app.model.CharityCase.Urgency.URGENT THEN 3
               WHEN com.charity.app.model.CharityCase.Urgency.HIGH THEN 2
               WHEN com.charity.app.model.CharityCase.Urgency.MEDIUM THEN 1
               ELSE 0 END DESC, c.createdAt ASC
           """)
    Page<CharityCase> findVisible(@Param("statuses") List<Status> statuses, Pageable pageable);

    @Query("""
           SELECT c FROM CharityCase c
           WHERE c.status IN :statuses AND c.category.id = :categoryId
           ORDER BY CASE c.urgency
               WHEN com.charity.app.model.CharityCase.Urgency.URGENT THEN 3
               WHEN com.charity.app.model.CharityCase.Urgency.HIGH THEN 2
               WHEN com.charity.app.model.CharityCase.Urgency.MEDIUM THEN 1
               ELSE 0 END DESC, c.createdAt ASC
           """)
    Page<CharityCase> findByCategoryIdAndVisible(@Param("categoryId") Long categoryId,
                                                 @Param("statuses") List<Status> statuses, Pageable pageable);

    @Query("""
           SELECT c FROM CharityCase c
           WHERE c.status IN :statuses AND
               (LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(c.description) LIKE LOWER(CONCAT('%', :q, '%')))
           ORDER BY CASE c.urgency
               WHEN com.charity.app.model.CharityCase.Urgency.URGENT THEN 3
               WHEN com.charity.app.model.CharityCase.Urgency.HIGH THEN 2
               WHEN com.charity.app.model.CharityCase.Urgency.MEDIUM THEN 1
               ELSE 0 END DESC, c.createdAt ASC
           """)
    Page<CharityCase> searchVisible(@Param("q") String q,
                                    @Param("statuses") List<Status> statuses, Pageable pageable);

    @Query("""
           SELECT c FROM CharityCase c
           WHERE c.status IN :statuses
             AND (:provinceId IS NULL OR c.center.province.id = :provinceId)
             AND (:cityId IS NULL OR c.center.city.id = :cityId)
           ORDER BY CASE c.urgency
               WHEN com.charity.app.model.CharityCase.Urgency.URGENT THEN 3
               WHEN com.charity.app.model.CharityCase.Urgency.HIGH THEN 2
               WHEN com.charity.app.model.CharityCase.Urgency.MEDIUM THEN 1
               ELSE 0 END DESC, c.createdAt ASC
           """)
    Page<CharityCase> findVisibleWithLocation(@Param("statuses") List<Status> statuses,
                                              @Param("provinceId") Long provinceId,
                                              @Param("cityId") Long cityId,
                                              Pageable pageable);

    @Query("""
           SELECT c FROM CharityCase c
           WHERE c.status IN :statuses
             AND (:provinceId IS NULL OR c.center.province.id = :provinceId)
             AND (:cityId IS NULL OR c.center.city.id = :cityId)
             AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(c.description) LIKE LOWER(CONCAT('%', :q, '%')))
           ORDER BY CASE c.urgency
               WHEN com.charity.app.model.CharityCase.Urgency.URGENT THEN 3
               WHEN com.charity.app.model.CharityCase.Urgency.HIGH THEN 2
               WHEN com.charity.app.model.CharityCase.Urgency.MEDIUM THEN 1
               ELSE 0 END DESC, c.createdAt ASC
           """)
    Page<CharityCase> searchVisibleWithLocation(@Param("q") String q,
                                                @Param("statuses") List<Status> statuses,
                                                @Param("provinceId") Long provinceId,
                                                @Param("cityId") Long cityId,
                                                Pageable pageable);

    @Query("""
           SELECT c FROM CharityCase c
           WHERE c.status IN :statuses AND c.category.id = :categoryId
             AND (:provinceId IS NULL OR c.center.province.id = :provinceId)
             AND (:cityId IS NULL OR c.center.city.id = :cityId)
           ORDER BY CASE c.urgency
               WHEN com.charity.app.model.CharityCase.Urgency.URGENT THEN 3
               WHEN com.charity.app.model.CharityCase.Urgency.HIGH THEN 2
               WHEN com.charity.app.model.CharityCase.Urgency.MEDIUM THEN 1
               ELSE 0 END DESC, c.createdAt ASC
           """)
    Page<CharityCase> findByCategoryIdAndVisibleWithLocation(@Param("categoryId") Long categoryId,
                                                               @Param("statuses") List<Status> statuses,
                                                               @Param("provinceId") Long provinceId,
                                                               @Param("cityId") Long cityId,
                                                               Pageable pageable);

    @Query("""
           SELECT c FROM CharityCase c
           WHERE c.center.id IN :centerIds AND c.status IN :statuses
           ORDER BY c.createdAt DESC
           """)
    Page<CharityCase> findByCenterIdIn(@Param("centerIds") List<Long> centerIds,
                                       @Param("statuses") List<Status> statuses,
                                       Pageable pageable);
}
