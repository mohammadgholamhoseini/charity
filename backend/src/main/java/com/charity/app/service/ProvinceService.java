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
public class ProvinceService {

    private final ProvinceRepository provinceRepository;

    @Transactional(readOnly = true)
    public List<Province> list(String q) {
        if (q != null && !q.isBlank()) {
            return provinceRepository.findByNameContainingIgnoreCase(q);
        }
        return provinceRepository.findAll();
    }

    @Transactional
    public Province create(NameRequest req) {
        Province p = Province.builder().name(req.getName()).build();
        return provinceRepository.save(p);
    }

    @Transactional
    public Province update(Long id, NameRequest req) {
        Province p = provinceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("استان یافت نشد"));
        p.setName(req.getName());
        return provinceRepository.save(p);
    }

    @Transactional
    public void delete(Long id) {
        provinceRepository.deleteById(id);
    }
}
