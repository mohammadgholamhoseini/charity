package com.charity.app.repository;

import com.charity.app.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByProvinceId(Long provinceId);
    List<City> findByProvinceIdAndNameContainingIgnoreCase(Long provinceId, String name);
}
