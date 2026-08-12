package com.charity.app.service;

import com.charity.app.common.error.ConflictException;
import com.charity.app.common.error.NotFoundException;
import com.charity.app.model.Province;
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
public class ProvinceService {

    private final ProvinceRepository provinces;
    private final CityRepository cities;
    private final CenterRepository centers;

    @Transactional(readOnly = true)
    public List<Province> list(String q) {
        return (q == null || q.isBlank())
                ? provinces.findAllByOrderByNameAsc()
                : provinces.findByNameContainingIgnoreCaseOrderByNameAsc(q);
    }

    @Transactional
    public Province create(NameRequest req) {
        if (provinces.existsByName(req.name())) {
            throw new ConflictException("PROVINCE_EXISTS", "این استان قبلاً ثبت شده است");
        }
        return provinces.save(Province.builder().name(req.name()).build());
    }

    @Transactional
    public Province update(Long id, NameRequest req) {
        Province province = load(id);
        province.setName(req.name());
        return provinces.save(province);
    }

    /** Refuses to delete a province that still has cities or centres attached to it. */
    @Transactional
    public void delete(Long id) {
        Province province = load(id);
        long cityCount = cities.countByProvinceId(id);
        if (cityCount > 0) {
            throw new ConflictException("PROVINCE_HAS_CITIES",
                    "این استان %d شهر ثبت‌شده دارد و قابل حذف نیست".formatted(cityCount));
        }
        if (centers.countByProvinceId(id) > 0) {
            throw new ConflictException("PROVINCE_IN_USE",
                    "این استان برای یک یا چند مرکز خیریه ثبت شده است و قابل حذف نیست");
        }
        provinces.delete(province);
    }

    private Province load(Long id) {
        return provinces.findById(id).orElseThrow(() -> new NotFoundException("استان یافت نشد"));
    }
}
