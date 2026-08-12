package com.charity.app.service;

import com.charity.app.common.SlugUtil;
import com.charity.app.common.error.ConflictException;
import com.charity.app.common.error.NotFoundException;
import com.charity.app.common.error.ValidationException;
import com.charity.app.mapper.CenterMapper;
import com.charity.app.model.Category;
import com.charity.app.model.Center;
import com.charity.app.model.City;
import com.charity.app.model.User;
import com.charity.app.model.enums.CenterStatus;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.UserRole;
import com.charity.app.payload.*;
import com.charity.app.repository.*;
import com.charity.app.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class CenterService {

    private final CenterRepository centers;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final CityRepository cities;
    private final RequestRepository requests;
    private final RequestService requestService;
    private final CenterMapper mapper;
    private final CurrentUser currentUser;
    private final PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------ public reads

    @Transactional(readOnly = true)
    public Page<CenterCard> publicList(Pageable pageable) {
        Page<Center> page = centers.findByStatus(CenterStatus.APPROVED, pageable);
        Map<Long, Long> counts = activeCounts(page.getContent());
        return page.map(center -> mapper.toCard(center, counts.getOrDefault(center.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public CenterPublicProfile publicProfile(String slug) {
        Center center = centers.findBySlug(slug)
                .filter(c -> c.getStatus() == CenterStatus.APPROVED)
                .orElseThrow(() -> new NotFoundException("مرکز خیریه یافت نشد"));
        return mapper.toPublicProfile(center, activeCount(center.getId()));
    }

    // ------------------------------------------------------------------ centre self-service

    @Transactional(readOnly = true)
    public CenterResponse currentCenter() {
        Center center = currentUser.center();
        return mapper.toResponse(center, activeCount(center.getId()));
    }

    @Transactional
    public CenterResponse updateOwnProfile(UpdateCenterProfileRequest req) {
        Center center = currentUser.center();
        center.setName(req.centerName());
        applySlugIfNameChanged(center);
        center.setFullName(req.fullName());
        center.setDescription(req.description());
        center.setContactPhone(req.contactPhone());
        center.setResponseHours(req.responseHours());
        center.setAddress(req.address());
        center.setCardNumber(req.cardNumber());
        center.setSheba(req.sheba());
        if (req.logoUrl() != null) {
            center.setLogoUrl(req.logoUrl());
        }
        if (req.cityId() != null) {
            assignCity(center, req.cityId());
        }
        Center saved = centers.save(center);
        return mapper.toResponse(saved, activeCount(saved.getId()));
    }

    @Transactional
    public CenterResponse setOwnLogo(String filename) {
        Center center = currentUser.center();
        center.setLogoUrl(filename);
        Center saved = centers.save(center);
        return mapper.toResponse(saved, activeCount(saved.getId()));
    }

    // ------------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    public Page<CenterResponse> adminList(Pageable pageable) {
        Page<Center> page = centers.findAllBy(pageable);
        Map<Long, Long> counts = activeCounts(page.getContent());
        return page.map(center -> mapper.toResponse(center, counts.getOrDefault(center.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public CenterResponse adminGet(Long id) {
        Center center = load(id);
        return mapper.toResponse(center, activeCount(id));
    }

    @Transactional
    public CenterResponse createByAdmin(CreateCenterByAdminRequest req) {
        if (users.existsByUsername(req.username())) {
            throw new ConflictException("USERNAME_TAKEN", "این نام کاربری قبلاً ثبت شده است");
        }
        if (users.existsByEmail(req.email())) {
            throw new ConflictException("EMAIL_TAKEN", "این ایمیل قبلاً ثبت شده است");
        }

        User user = users.save(User.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .email(req.email())
                .role(UserRole.CENTER)
                .fullName(req.fullName())
                .enabled(true)
                .build());

        Center center = Center.builder()
                .user(user)
                .name(req.centerName())
                .fullName(req.fullName())
                .categories(resolveCategories(req.categoryIds()))
                .description(req.description())
                .contactPhone(req.contactPhone())
                .responseHours(req.responseHours())
                .address(req.address())
                .cardNumber(req.cardNumber())
                .sheba(req.sheba())
                // Admin-created means already approved: there is no application to review.
                .status(Boolean.FALSE.equals(req.active()) ? CenterStatus.INACTIVE : CenterStatus.APPROVED)
                .build();
        assignCity(center, req.cityId());
        center.setSlug(uniqueSlug(req.centerName(), null));

        return mapper.toResponse(centers.save(center), 0);
    }

    @Transactional
    public CenterResponse updateByAdmin(Long id, UpdateCenterByAdminRequest req) {
        Center center = load(id);

        center.setName(req.centerName());
        applySlugIfNameChanged(center);
        center.setFullName(req.fullName());
        if (center.getUser() != null && req.fullName() != null) {
            center.getUser().setFullName(req.fullName());
        }
        center.setDescription(req.description());
        center.setContactPhone(req.contactPhone());
        center.setResponseHours(req.responseHours());
        center.setAddress(req.address());
        center.setCardNumber(req.cardNumber());
        center.setSheba(req.sheba());
        assignCity(center, req.cityId());

        if (req.categoryIds() != null) {
            replaceCategories(center, req.categoryIds());
        }
        if (req.active() != null) {
            applyActive(center, req.active());
        }

        Center saved = centers.save(center);
        return mapper.toResponse(saved, activeCount(saved.getId()));
    }

    @Transactional
    public CenterResponse setCategories(Long id, SetCategoriesDto dto) {
        Center center = load(id);
        replaceCategories(center, dto.categoryIds());
        Center saved = centers.save(center);
        return mapper.toResponse(saved, activeCount(saved.getId()));
    }

    @Transactional
    public CenterResponse setActive(Long id, boolean active) {
        Center center = load(id);
        applyActive(center, active);
        Center saved = centers.save(center);
        return mapper.toResponse(saved, activeCount(saved.getId()));
    }

    @Transactional
    public void delete(Long id) {
        Center center = load(id);
        long owned = requests.countByCenterIdAndDeletedAtIsNull(id);
        if (owned > 0) {
            throw new ConflictException("CENTER_HAS_REQUESTS",
                    ("این مرکز %d درخواست ثبت‌شده دارد و قابل حذف نیست. "
                            + "برای خارج کردن آن از سایت، وضعیت مرکز را غیرفعال کنید.").formatted(owned));
        }
        User user = center.getUser();
        center.setUser(null);
        centers.delete(center);
        if (user != null) {
            users.delete(user);
        }
    }

    // ------------------------------------------------------------------ internals

    private void applyActive(Center center, boolean active) {
        center.setStatus(active ? CenterStatus.APPROVED : CenterStatus.INACTIVE);
        if (!active) {
            // A centre leaving the site must take its live requests with it, otherwise visitors are
            // pointed at a contact who is no longer reachable.
            requestService.deactivateAllForCenter(center.getId());
        }
    }

    /**
     * Refuses to strip a category the centre still has requests in -- those requests would be left
     * referencing a category their centre is no longer permitted to publish in.
     */
    private void replaceCategories(Center center, List<Long> categoryIds) {
        Set<Category> resolved = resolveCategories(categoryIds);
        Set<Long> keptIds = resolved.stream().map(Category::getId).collect(java.util.stream.Collectors.toSet());

        Set<Long> inUse = new HashSet<>();
        requests.findAll(
                        com.charity.app.repository.spec.RequestSpecifications.notDeleted()
                                .and(com.charity.app.repository.spec.RequestSpecifications
                                        .centerIdEquals(center.getId())),
                        Pageable.unpaged())
                .forEach(request -> inUse.add(request.getCategory().getId()));

        List<Long> orphaned = inUse.stream().filter(idInUse -> !keptIds.contains(idInUse)).toList();
        if (!orphaned.isEmpty()) {
            throw new ConflictException("CATEGORY_IN_USE_BY_CENTER",
                    "این مرکز در %d دسته‌بندی حذف‌شده درخواست ثبت‌شده دارد؛ ابتدا آن درخواست‌ها را جابه‌جا کنید."
                            .formatted(orphaned.size()));
        }
        center.setCategories(resolved);
    }

    private Set<Category> resolveCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new ValidationException("حداقل یک دسته‌بندی مجاز باید انتخاب شود");
        }
        Set<Category> resolved = new HashSet<>();
        for (Long categoryId : categoryIds) {
            resolved.add(categories.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("دسته‌بندی یافت نشد")));
        }
        return resolved;
    }

    /** Province is derived from the city rather than being set independently, so the two cannot disagree. */
    private void assignCity(Center center, Long cityId) {
        if (cityId == null) {
            return;
        }
        City city = cities.findById(cityId).orElseThrow(() -> new NotFoundException("شهر یافت نشد"));
        center.setCity(city);
        center.setProvince(city.getProvince());
    }

    private void applySlugIfNameChanged(Center center) {
        if (center.getSlug() == null || center.getSlug().isBlank()) {
            center.setSlug(uniqueSlug(center.getName(), center.getId()));
        }
    }

    /**
     * Centre slugs are admin-managed and low volume, so they get a clean name-based slug with a
     * numeric suffix only on collision -- unlike request slugs, which carry their code.
     */
    private String uniqueSlug(String name, Long currentId) {
        String base = SlugUtil.slugify(name, 180);
        if (base.isEmpty()) {
            base = "center";
        }
        String candidate = base;
        int suffix = 2;
        while (isSlugTaken(candidate, currentId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean isSlugTaken(String slug, Long currentId) {
        return centers.findBySlug(slug).map(c -> !c.getId().equals(currentId)).orElse(false);
    }

    private Center load(Long id) {
        return centers.findById(id).orElseThrow(() -> new NotFoundException("مرکز خیریه یافت نشد"));
    }

    private long activeCount(Long centerId) {
        return activeCounts(List.of(centerId), Function.identity()).getOrDefault(centerId, 0L);
    }

    private Map<Long, Long> activeCounts(List<Center> page) {
        return activeCounts(page.stream().map(Center::getId).toList(), Function.identity());
    }

    /** One grouped query per page rather than a count per centre. */
    private Map<Long, Long> activeCounts(List<Long> centerIds, Function<Long, Long> identity) {
        if (centerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        requests.countActiveByCenterIds(RequestStatus.PUBLISHED, centerIds)
                .forEach(row -> counts.put(row.getCenterId(), row.getTotal()));
        return counts;
    }
}
