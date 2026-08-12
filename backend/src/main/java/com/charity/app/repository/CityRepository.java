package com.charity.app.repository;

import com.charity.app.model.City;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    @EntityGraph(attributePaths = "province")
    List<City> findByProvinceIdOrderByNameAsc(Long provinceId);

    @EntityGraph(attributePaths = "province")
    List<City> findByProvinceIdAndNameContainingIgnoreCase(Long provinceId, String name);

    @EntityGraph(attributePaths = "province")
    List<City> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    @EntityGraph(attributePaths = "province")
    List<City> findAllByOrderByNameAsc();

    boolean existsByProvinceIdAndName(Long provinceId, String name);

    long countByProvinceId(Long provinceId);
}
