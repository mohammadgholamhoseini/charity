package com.charity.app.service;

import com.charity.app.model.Category;
import com.charity.app.model.Center;
import com.charity.app.model.CharityCase;
import com.charity.app.model.User;
import com.charity.app.payload.CreateCaseRequest;
import com.charity.app.payload.UpdateCaseRequest;
import com.charity.app.payload.CharityCaseResponse;
import com.charity.app.repository.CategoryRepository;
import com.charity.app.repository.CenterRepository;
import com.charity.app.repository.CharityCaseRepository;
import com.charity.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CharityCaseService {

    private final CharityCaseRepository caseRepository;
    private final CenterRepository centerRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final List<MessagingService> messagingServices;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional
    public CharityCaseResponse createCase(CreateCaseRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("کاربر یافت نشد"));
        Center center = centerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NoSuchElementException("مرکز خیریه یافت نشد. ابتدا اطلاعات مرکز ثبت شود"));

        if (center.getStatus() != Center.Status.APPROVED) {
            throw new IllegalStateException("مرکز شما هنوز تایید نشده است");
        }

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new NoSuchElementException("دسته‌بندی یافت نشد"));
        if (center.getCategories() == null || center.getCategories().stream()
                .noneMatch(c -> c.getId().equals(category.getId()))) {
            throw new IllegalArgumentException("این دسته‌بندی در لیست مجاز مرکز شما نیست");
        }

        CharityCase c = CharityCase.builder()
                .center(center)
                .category(category)
                .title(req.getTitle())
                .description(req.getDescription())
                .amountNeeded(req.getAmountNeeded())
                .amountCollected(java.math.BigDecimal.ZERO)
                .imageUrl(req.getImageUrl())
                .contactInfo(req.getContactInfo())
                .urgency(parseUrgency(req.getUrgency()))
                .status(CharityCase.Status.PUBLISHED)
                .build();
        c.setDetails(req.getDetails());
        c = caseRepository.save(c);

        c = publishToMessagingServices(c);
        return toResponse(c);
    }

    @Transactional
    public CharityCase markCompleted(Long caseId) {
        CharityCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("درخواست یافت نشد"));
        c.setStatus(CharityCase.Status.COMPLETED);
        return caseRepository.save(c);
    }

    @Transactional
    public CharityCaseResponse updateCase(Long caseId, UpdateCaseRequest req) {
        CharityCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("درخواست یافت نشد"));
        if (c.getStatus() == CharityCase.Status.COMPLETED) {
            throw new IllegalStateException("درخواست تأمین شده قابل ویرایش نیست");
        }
        if (c.getCenter() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new NoSuchElementException("دسته‌بندی یافت نشد"));
            if (c.getCenter().getCategories() == null || c.getCenter().getCategories().stream()
                    .noneMatch(cat -> cat.getId().equals(category.getId()))) {
                throw new IllegalArgumentException("این دسته‌بندی در لیست مجاز مرکز شما نیست");
            }
            c.setCategory(category);
        } else {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new NoSuchElementException("دسته‌بندی یافت نشد"));
            c.setCategory(category);
        }
        c.setTitle(req.getTitle());
        c.setDescription(req.getDescription());
        c.setAmountNeeded(req.getAmountNeeded());
        c.setImageUrl(req.getImageUrl());
        c.setContactInfo(req.getContactInfo());
        if (req.getUrgency() != null && !req.getUrgency().isBlank()) {
            c.setUrgency(parseUrgency(req.getUrgency()));
        }
        c.setDetails(req.getDetails());
        return toResponse(caseRepository.save(c));
    }

    @Transactional
    public void deleteCase(Long caseId) {
        CharityCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("درخواست یافت نشد"));
        if (c.getStatus() == CharityCase.Status.COMPLETED) {
            throw new IllegalStateException("درخواست تأمین شده قابل حذف نیست");
        }
        caseRepository.delete(c);
    }

    @Transactional
    public void deactivateByCenter(Long centerId) {
        caseRepository.findByCenterIdAndStatusNot(centerId, CharityCase.Status.COMPLETED)
                .forEach(c -> {
                    c.setStatus(CharityCase.Status.INACTIVE);
                    caseRepository.save(c);
                });
    }

    @Transactional
    public CharityCase addDocuments(Long caseId, java.util.List<String> filenames) {
        CharityCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("درخواست یافت نشد"));
        java.util.List<String> docs = new java.util.ArrayList<>(c.getDocuments());
        docs.addAll(filenames);
        c.setDocuments(docs);
        return caseRepository.save(c);
    }

    @Transactional
    public CharityCase publishToMessagingServices(CharityCase c) {
        for (MessagingService service : messagingServices) {
            if (!service.isEnabled()) continue;
            Integer msgId = service.publishCase(c);
            if (msgId != null) {
                String name = service.getName();
                if ("telegram".equals(name)) {
                    c.setTelegramPosted(true);
                    c.setTelegramMessageId(msgId);
                } else if ("bale".equals(name)) {
                    c.setBalePosted(true);
                    c.setBaleMessageId(msgId);
                }
                c = caseRepository.save(c);
            }
        }
        return c;
    }

    private static final java.util.List<CharityCase.Status> VISIBLE =
            java.util.List.of(CharityCase.Status.PUBLISHED, CharityCase.Status.COMPLETED);

    @Transactional(readOnly = true)
    public Page<CharityCaseResponse> publicList(Pageable pageable, String q, Long categoryId,
                                                Long provinceId, Long cityId, Long centerId) {
        Page<CharityCase> page;
        boolean hasLocation = provinceId != null || cityId != null;
        if (centerId != null) {
            page = caseRepository.findByCenterIdIn(java.util.List.of(centerId), VISIBLE, pageable);
        } else if (categoryId != null && hasLocation) {
            page = caseRepository.findByCategoryIdAndVisibleWithLocation(categoryId, VISIBLE, provinceId, cityId, pageable);
        } else if (categoryId != null) {
            page = caseRepository.findByCategoryIdAndVisible(categoryId, VISIBLE, pageable);
        } else if (q != null && !q.isBlank() && hasLocation) {
            page = caseRepository.searchVisibleWithLocation(q, VISIBLE, provinceId, cityId, pageable);
        } else if (q != null && !q.isBlank()) {
            page = caseRepository.searchVisible(q, VISIBLE, pageable);
        } else if (hasLocation) {
            page = caseRepository.findVisibleWithLocation(VISIBLE, provinceId, cityId, pageable);
        } else {
            page = caseRepository.findVisible(VISIBLE, pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CharityCaseResponse> centerList(Pageable pageable) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Center center = centerRepository.findByUserId(user.getId()).orElseThrow();
        return caseRepository.findByCenterIdOrderByCreatedAtDesc(center.getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CharityCaseResponse> adminList(Pageable pageable, String status) {
        Page<CharityCase> page;
        if (status != null && !status.isBlank()) {
            page = caseRepository.findByStatusOrderByCreatedAtDesc(
                    CharityCase.Status.valueOf(status.toUpperCase()), pageable);
        } else {
            page = caseRepository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CharityCaseResponse getPublic(Long id) {
        CharityCase c = caseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("درخواست یافت نشد"));
        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public void ensureOwnedByCurrentCenter(Long caseId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Center center = centerRepository.findByUserId(user.getId()).orElseThrow();
        CharityCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("درخواست یافت نشد"));
        if (c.getCenter() == null || !c.getCenter().getId().equals(center.getId())) {
            throw new IllegalStateException("این درخواست متعلق به مرکز شما نیست");
        }
    }

    private CharityCase.Urgency parseUrgency(String value) {
        if (value == null || value.isBlank()) {
            return CharityCase.Urgency.MEDIUM;
        }
        try {
            return CharityCase.Urgency.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CharityCase.Urgency.MEDIUM;
        }
    }

    private CharityCaseResponse toResponse(CharityCase c) {
        CharityCaseResponse r = new CharityCaseResponse();
        r.setId(c.getId());
        r.setTitle(c.getTitle());
        r.setDescription(c.getDescription());
        r.setAmountNeeded(c.getAmountNeeded());
        r.setAmountCollected(c.getAmountCollected());
        r.setImageUrl(c.getImageUrl());
        r.setContactInfo(c.getContactInfo());
        r.setDetails(c.getDetails());
        r.setDocuments(c.getDocuments());
        r.setStatus(c.getStatus().name());
        r.setUrgency(c.getUrgency() != null ? c.getUrgency().name() : CharityCase.Urgency.MEDIUM.name());
        if (c.getCenter() != null) {
            r.setCenterId(c.getCenter().getId());
        }
        r.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : null);
        if (c.getCenter() != null) {
            r.setCenterName(c.getCenter().getName());
            if (c.getCenter().getPrimaryCategory() != null) {
                r.setCenterCategory(c.getCenter().getPrimaryCategory().getName());
            }
        }
        if (c.getCategory() != null) {
            r.setCategoryId(c.getCategory().getId());
            r.setCategoryName(c.getCategory().getName());
        }
        return r;
    }
}
