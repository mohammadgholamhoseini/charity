package com.charity.app.service;

import com.charity.app.common.error.ConflictException;
import com.charity.app.common.error.NotFoundException;
import com.charity.app.mapper.LocationMapper;
import com.charity.app.model.City;
import com.charity.app.model.Province;
import com.charity.app.payload.CityRef;
import com.charity.app.payload.NameRequest;
import com.charity.app.repository.CenterRepository;
import com.charity.app.repository.CityRepository;
import com.charity.app.repository.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cities;
    private final ProvinceRepository provinces;
    private final CenterRepository centers;
    private final LocationMapper mapper;

    /** Returns flattened refs rather than entities, so City.province no longer has to be EAGER. */
    @Transactional(readOnly = true)
    public List<CityRef> list(Long provinceId, String q) {
        List<City> found;
        if (provinceId != null && q != null && !q.isBlank()) {
            found = cities.findByProvinceIdAndNameContainingIgnoreCase(provinceId, q);
        } else if (provinceId != null) {
            found = cities.findByProvinceIdOrderByNameAsc(provinceId);
        } else if (q != null && !q.isBlank()) {
            found = cities.findByNameContainingIgnoreCaseOrderByNameAsc(q);
        } else {
            found = cities.findAllByOrderByNameAsc();
        }
        return found.stream().map(mapper::toRef).toList();
    }

    @Transactional
    public CityRef create(Long provinceId, NameRequest req) {
        Province province = provinces.findById(provinceId)
                .orElseThrow(() -> new NotFoundException("استان یافت نشد"));
        if (cities.existsByProvinceIdAndName(provinceId, req.name())) {
            throw new ConflictException("CITY_EXISTS", "این شهر قبلاً در این استان ثبت شده است");
        }
        return mapper.toRef(cities.save(City.builder().name(req.name()).province(province).build()));
    }

    @Transactional
    public CityRef update(Long id, NameRequest req) {
        City city = load(id);
        city.setName(req.name());
        return mapper.toRef(cities.save(city));
    }

    /**
     * Refuses to delete a city that is still referenced. Previously this was a bare
     * {@code deleteById} that failed on the foreign key and surfaced as a 500 with raw SQL.
     */
    @Transactional
    public void delete(Long id) {
        City city = load(id);
        long centerCount = centers.countByCityId(id);
        if (centerCount > 0) {
            throw new ConflictException("CITY_IN_USE",
                    "این شهر برای %d مرکز خیریه ثبت شده است و قابل حذف نیست".formatted(centerCount));
        }
        cities.delete(city);
    }

    private City load(Long id) {
        return cities.findById(id).orElseThrow(() -> new NotFoundException("شهر یافت نشد"));
    }
}
