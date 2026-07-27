package com.charity.app.service;

import com.charity.app.model.Category;
import com.charity.app.model.Center;
import com.charity.app.model.City;
import com.charity.app.model.Province;
import com.charity.app.model.User;
import com.charity.app.payload.CenterResponse;
import com.charity.app.payload.CreateCenterByAdminRequest;
import com.charity.app.payload.UpdateCenterByAdminRequest;
import com.charity.app.payload.UpdateCenterProfileRequest;
import com.charity.app.repository.CategoryRepository;
import com.charity.app.repository.CenterRepository;
import com.charity.app.repository.CityRepository;
import com.charity.app.repository.ProvinceRepository;
import com.charity.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CenterService {

    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProvinceRepository provinceRepository;
    private final CityRepository cityRepository;
    private final CharityCaseService caseService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Center createByAdmin(CreateCenterByAdminRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("نام کاربری قبلاً ثبت شده است");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("ایمیل قبلاً ثبت شده است");
        }

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(req.getEmail())
                .role(User.Role.CENTER)
                .fullName(req.getFullName())
                .enabled(true)
                .build();
        user = userRepository.save(user);

        Set<Category> categories = new HashSet<>();
        if (req.getCategoryIds() != null) {
            for (Long cid : req.getCategoryIds()) {
                categoryRepository.findById(cid).ifPresent(categories::add);
            }
        }

        Province province = req.getProvinceId() != null
                ? provinceRepository.findById(req.getProvinceId()).orElse(null) : null;
        City city = req.getCityId() != null
                ? cityRepository.findById(req.getCityId()).orElse(null) : null;

        Center center = Center.builder()
                .user(user)
                .name(req.getCenterName())
                .fullName(req.getFullName())
                .categories(categories)
                .province(province)
                .city(city)
                .description(req.getDescription())
                .contactPhone(req.getContactPhone())
                .address(req.getAddress())
                .cardNumber(req.getCardNumber())
                .sheba(req.getSheba())
                .status(Center.Status.APPROVED)
                .build();
        return centerRepository.save(center);
    }

    @Transactional
    public Center updateByAdmin(Long id, UpdateCenterByAdminRequest req) {
        Center c = centerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("مرکز یافت نشد"));
        if (req.getCenterName() != null) c.setName(req.getCenterName());
        if (req.getFullName() != null) {
            c.setFullName(req.getFullName());
            if (c.getUser() != null) c.getUser().setFullName(req.getFullName());
        }
        if (req.getDescription() != null) c.setDescription(req.getDescription());
        if (req.getContactPhone() != null) c.setContactPhone(req.getContactPhone());
        if (req.getAddress() != null) c.setAddress(req.getAddress());
        if (req.getCardNumber() != null) c.setCardNumber(req.getCardNumber());
        if (req.getSheba() != null) c.setSheba(req.getSheba());
        if (req.getProvinceId() != null) {
            c.setProvince(provinceRepository.findById(req.getProvinceId()).orElse(null));
        }
        if (req.getCityId() != null) {
            c.setCity(cityRepository.findById(req.getCityId()).orElse(null));
        }
        if (req.getCategoryIds() != null) {
            Set<Category> categories = new HashSet<>();
            for (Long cid : req.getCategoryIds()) {
                categoryRepository.findById(cid).ifPresent(categories::add);
            }
            c.setCategories(categories);
        }
        return centerRepository.save(c);
    }

    @Transactional
    public void delete(Long id) {
        Center c = centerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("مرکز یافت نشد"));
        User user = c.getUser();
        c.setUser(null);
        centerRepository.delete(c);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Transactional
    public Center deactivate(Long id) {
        Center c = centerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("مرکز یافت نشد"));
        c.setStatus(Center.Status.INACTIVE);
        Center saved = centerRepository.save(c);
        caseService.deactivateByCenter(id);
        return saved;
    }

    @Transactional
    public Center activate(Long id) {
        Center c = centerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("مرکز یافت نشد"));
        c.setStatus(Center.Status.APPROVED);
        return centerRepository.save(c);
    }

    @Transactional
    public Center updateOwnProfile(User currentUser, UpdateCenterProfileRequest req) {
        Center c = centerRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NoSuchElementException("مرکز یافت نشد"));
        if (req.getCenterName() != null) c.setName(req.getCenterName());
        if (req.getFullName() != null) {
            c.setFullName(req.getFullName());
            if (c.getUser() != null) c.getUser().setFullName(req.getFullName());
        }
        if (req.getDescription() != null) c.setDescription(req.getDescription());
        if (req.getContactPhone() != null) c.setContactPhone(req.getContactPhone());
        if (req.getAddress() != null) c.setAddress(req.getAddress());
        if (req.getCardNumber() != null) c.setCardNumber(req.getCardNumber());
        if (req.getSheba() != null) c.setSheba(req.getSheba());
        if (req.getLogoUrl() != null) c.setLogoUrl(req.getLogoUrl());
        if (req.getProvinceId() != null) {
            c.setProvince(provinceRepository.findById(req.getProvinceId()).orElse(null));
        }
        if (req.getCityId() != null) {
            c.setCity(cityRepository.findById(req.getCityId()).orElse(null));
        }
        return centerRepository.save(c);
    }

    @Transactional
    public Center setLogo(User currentUser, String logoUrl) {
        Center c = centerRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NoSuchElementException("مرکز یافت نشد"));
        c.setLogoUrl(logoUrl);
        return centerRepository.save(c);
    }

    @Transactional
    public Center setCategories(Long centerId, List<Long> categoryIds) {
        Center c = centerRepository.findById(centerId)
                .orElseThrow(() -> new NoSuchElementException("مرکز یافت نشد"));
        Set<Category> categories = new HashSet<>();
        if (categoryIds != null) {
            for (Long cid : categoryIds) {
                categoryRepository.findById(cid).ifPresent(categories::add);
            }
        }
        c.setCategories(categories);
        return centerRepository.save(c);
    }

    @Transactional
    public Center approve(Long centerId) {
        Center c = centerRepository.findById(centerId)
                .orElseThrow(() -> new NoSuchElementException("مرکز یافت نشد"));
        c.setStatus(Center.Status.APPROVED);
        return centerRepository.save(c);
    }

    @Transactional(readOnly = true)
    public Page<Center> listPending(Pageable pageable) {
        return centerRepository.findByStatus(Center.Status.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Center> listAll(Pageable pageable) {
        return centerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<CenterResponse> listByStatus(Center.Status status, Pageable pageable) {
        return centerRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CenterResponse> listAllResponse(Pageable pageable) {
        return centerRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CenterResponse> listPendingResponse(Pageable pageable) {
        return centerRepository.findByStatus(Center.Status.PENDING, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CenterResponse getByIdResponse(Long id) {
        Center c = centerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("مرکز یافت نشد"));
        return toResponse(c);
    }

    private CenterResponse toResponse(Center c) {
        CenterResponse r = new CenterResponse();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setFullName(c.getFullName());
        r.setDescription(c.getDescription());
        r.setContactPhone(c.getContactPhone());
        r.setAddress(c.getAddress());
        r.setCardNumber(c.getCardNumber());
        r.setSheba(c.getSheba());
        r.setLogoUrl(c.getLogoUrl());
        r.setStatus(c.getStatus().name());
        if (c.getUser() != null) r.setEmail(c.getUser().getEmail());
        if (c.getProvince() != null) {
            r.setProvince(new CenterResponse.ProvinceInfo(c.getProvince().getId(), c.getProvince().getName()));
        }
        if (c.getCity() != null) {
            r.setCity(new CenterResponse.CityInfo(c.getCity().getId(), c.getCity().getName()));
        }
        if (c.getCategories() != null) {
            r.setCategories(c.getCategories().stream()
                .map(cat -> new CenterResponse.CategoryInfo(cat.getId(), cat.getName()))
                .toList());
        }
        if (c.getCreatedAt() != null) r.setCreatedAt(c.getCreatedAt().toString());
        return r;
    }

    @Transactional(readOnly = true)
    public Center currentCenter() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        return centerRepository.findByUserId(user.getId()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public CenterResponse currentCenterResponse() {
        return toResponse(currentCenter());
    }

    @Transactional(readOnly = true)
    public Center getById(Long id) {
        return centerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("مرکز یافت نشد"));
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getPublicProfile(Long id) {
        Center c = centerRepository.findById(id)
                .filter(center -> center.getStatus() == Center.Status.APPROVED)
                .orElseThrow(() -> new NoSuchElementException("مرکز یافت نشد"));
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("name", c.getName());
        map.put("fullName", c.getFullName());
        map.put("description", c.getDescription());
        map.put("contactPhone", c.getContactPhone());
        map.put("address", c.getAddress());
        map.put("cardNumber", c.getCardNumber());
        map.put("sheba", c.getSheba());
        map.put("logoUrl", c.getLogoUrl());
        map.put("categories", c.getCategories());
        map.put("status", c.getStatus().name());
        if (c.getProvince() != null) {
            map.put("provinceId", c.getProvince().getId());
            map.put("provinceName", c.getProvince().getName());
        }
        if (c.getCity() != null) {
            map.put("cityId", c.getCity().getId());
            map.put("cityName", c.getCity().getName());
        }
        return map;
    }
}
