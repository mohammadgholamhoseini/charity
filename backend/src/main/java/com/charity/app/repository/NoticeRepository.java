package com.charity.app.repository;

import com.charity.app.model.Notice;
import com.charity.app.model.enums.NoticePlacement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * Announcements eligible to show right now for a placement, best first. Callers pass a
     * {@code PageRequest.of(0, 1)} because the design shows at most one per placement.
     */
    @Query("""
           SELECT n FROM Notice n
           WHERE n.active = true
             AND n.placement = :placement
             AND (n.startAt IS NULL OR n.startAt <= :now)
             AND (n.endAt   IS NULL OR n.endAt   >= :now)
           ORDER BY n.startAt DESC, n.createdAt DESC
           """)
    List<Notice> findServable(@Param("placement") NoticePlacement placement,
                              @Param("now") LocalDateTime now,
                              Pageable limit);

    List<Notice> findAllByOrderByCreatedAtDesc();

    List<Notice> findByPlacementOrderByCreatedAtDesc(NoticePlacement placement);
}
