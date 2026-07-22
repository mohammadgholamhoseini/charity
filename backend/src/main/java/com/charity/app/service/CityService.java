package com.charity.app.service;

import com.charity.app.model.City;
import com.charity.app.model.Province;
import com.charity.app.payload.NameRequest;
import com.charity.app.repository.CityRepository;
import com.charity.app.repository.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final ProvinceRepository provinceRepository;

    @Transactional(readOnly = true)
    public List<City> list(Long provinceId, String q) {
        if (provinceId != null) {
            if (q != null && !q.isBlank()) {
                return cityRepository.findByProvinceIdAndNameContainingIgnoreCase(provinceId, q);
            }
            return cityRepository.findByProvinceId(provinceId);
        }
        return cityRepository.findAll();
    }

    @Transactional
    public City create(Long provinceId, NameRequest req) {
        Province province = provinceRepository.findById(provinceId)
                .orElseThrow(() -> new NoSuchElementException("استان یافت نشد"));
        City c = City.builder().name(req.getName()).province(province).build();
        return cityRepository.save(c);
    }

    @Transactional
    public City update(Long id, NameRequest req) {
        City c = cityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("شهر یافت نشد"));
        c.setName(req.getName());
        return cityRepository.save(c);
    }

    @Transactional
    public void delete(Long id) {
        cityRepository.deleteById(id);
    }
}
