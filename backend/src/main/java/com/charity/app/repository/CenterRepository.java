package com.charity.app.repository;

import com.charity.app.model.Center;
import com.charity.app.model.Center.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CenterRepository extends JpaRepository<Center, Long> {
    Optional<Center> findByUserId(Long userId);
    Page<Center> findByStatus(Status status, Pageable pageable);
    boolean existsByName(String name);
}
