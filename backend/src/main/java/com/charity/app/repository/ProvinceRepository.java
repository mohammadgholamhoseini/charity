package com.charity.app.repository;

import com.charity.app.model.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {

    List<Province> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<Province> findAllByOrderByNameAsc();

    Optional<Province> findByName(String name);

    boolean existsByName(String name);
}
