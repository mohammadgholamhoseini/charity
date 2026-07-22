package com.charity.app.repository;

import com.charity.app.model.Notice;
import com.charity.app.model.Notice.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByActiveAndPositionOrderByCreatedAtDesc(boolean active, Position position);

    List<Notice> findAllByOrderByCreatedAtDesc();
}
