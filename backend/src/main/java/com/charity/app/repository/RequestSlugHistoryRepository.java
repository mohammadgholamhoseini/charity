package com.charity.app.repository;

import com.charity.app.model.RequestSlugHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RequestSlugHistoryRepository extends JpaRepository<RequestSlugHistory, Long> {

    Optional<RequestSlugHistory> findByOldSlug(String oldSlug);

    boolean existsByOldSlug(String oldSlug);
}
