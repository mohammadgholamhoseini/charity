package com.charity.app.service;

import com.charity.app.common.AppUrls;
import com.charity.app.common.SlugUtil;
import com.charity.app.common.error.ConflictException;
import com.charity.app.common.error.ForbiddenException;
import com.charity.app.common.error.GoneException;
import com.charity.app.common.error.NotFoundException;
import com.charity.app.common.error.ValidationException;
import com.charity.app.event.RequestPublishedEvent;
import com.charity.app.mapper.RequestMapper;
import com.charity.app.model.*;
import com.charity.app.model.enums.CenterStatus;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.Urgency;
import com.charity.app.payload.*;
import com.charity.app.repository.*;
import com.charity.app.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.charity.app.repository.spec.RequestSpecifications.*;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requests;
    private final RequestSlugHistoryRepository slugHistory;
    private final CenterRepository centers;
    private final CategoryRepository categories;
    private final CityRepository cities;
    private final RequestMapper mapper;
    private final RequestStatusPolicy statusPolicy;
    private final CurrentUser currentUser;
    private final AppUrls urls;
    private final ApplicationEventPublisher events;

    // ------------------------------------------------------------------ public reads

    /**
     * The public listing. Every facet in {@link RequestFilter} composes -- this one method replaces
     * seven mutually exclusive query paths.
     */
    @Transactional(readOnly = true)
    public Page<RequestSummary> publicList(RequestFilter filter, Pageable pageable) {
        Collection<RequestStatus> statuses = resolvePublicStatuses(filter.statuses());
        return requests.findAll(publicSpec(filter, statuses), pageable).map(mapper::toSummary);
    }

    /**
     * Resolves a slug to a request, distinguishing the three ways it can fail.
     *
     * <p>A COMPLETED request still returns 200: it drops out of the active listing, but its URL is
     * already indexed and linked from the messaging channels, and throwing that away would be
     * self-inflicted. Statuses that were never public are 404, and a soft-deleted request is 410 so
     * search engines drop it quickly.
     */
    @Transactional(readOnly = true)
    public RequestDetailResponse publicDetail(String slug) {
        Request request = requests.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> notFoundOrGone(slug));

        if (!request.getStatus().isPubliclyVisible()) {
            throw new NotFoundException("درخواست یافت نشد");
        }
        long activeAtCenter = requests.countByCenterIdAndDeletedAtIsNull(request.getCenter().getId());
        return mapper.toDetail(request, activeAtCenter, false);
    }

    /**
     * Canonical slug for an incoming one, or null when it is already canonical.
     * Lets the frontend answer 301 after a title edit rather than 404.
     */
    @Transactional(readOnly = true)
    public String canonicalSlugFor(String slug) {
        if (requests.existsBySlug(slug)) {
            return null;
        }
        return slugHistory.findByOldSlug(slug)
                .map(history -> history.getRequest().getSlug())
                .orElse(null);
    }

    /** Legacy {@code /case/{id}} links posted to Telegram and Bale resolve through here. */
    @Transactional(readOnly = true)
    public String slugForLegacyId(Long id) {
        return requests.findById(id)
                .filter(r -> !r.isDeleted() && r.getStatus().isPubliclyVisible())
                .map(Request::getSlug)
                .orElseThrow(() -> new NotFoundException("درخواست یافت نشد"));
    }

    @Transactional(readOnly = true)
    public String slugForCode(String code) {
        return requests.findByCodeIgnoreCase(code)
                .filter(r -> !r.isDeleted() && r.getStatus().isPubliclyVisible())
                .map(Request::getSlug)
                .orElseThrow(() -> new NotFoundException("درخواست یافت نشد"));
    }

    // ------------------------------------------------------------------ centre reads

    @Transactional(readOnly = true)
    public Page<RequestSummary> centerList(RequestFilter filter, Pageable pageable) {
        Center center = currentUser.center();
        Specification<Request> spec = Specification.allOf(
                notDeleted(),
                centerIdEquals(center.getId()),
                statusIn(filter.statuses()),
                categoryIdIn(filter.categoryIds()),
                urgencyIn(filter.urgencies()),
                textMatches(filter.query()));
        return requests.findAll(spec, pageable).map(mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public RequestDetailResponse centerDetail(Long id) {
        Request request = ownedByCurrentCenter(id);
        return mapper.toDetail(request, 0, true);
    }

    @Transactional(readOnly = true)
    public Map<RequestStatus, Long> centerStats() {
        Center center = currentUser.center();
        return toStatusMap(requests.countGroupedByStatusForCenter(center.getId()));
    }

    // ------------------------------------------------------------------ admin reads

    @Transactional(readOnly = true)
    public Page<RequestSummary> adminList(RequestFilter filter, Pageable pageable) {
        Specification<Request> spec = Specification.allOf(
                notDeleted(),
                statusIn(filter.statuses()),
                categoryIdIn(filter.categoryIds()),
                urgencyIn(filter.urgencies()),
                cityIdIn(filter.cityIds()),
                centerIdEquals(filter.centerId()),
                textMatches(filter.query()));
        return requests.findAll(spec, pageable).map(mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public RequestDetailResponse adminDetail(Long id) {
        return mapper.toDetail(loadById(id), 0, true);
    }

    /** Feeds the five stat cards. Every status is present, so the UI never has to null-check. */
    @Transactional(readOnly = true)
    public Map<RequestStatus, Long> adminStats() {
        return toStatusMap(requests.countGroupedByStatus());
    }

    // ------------------------------------------------------------------ writes

    @Transactional
    public RequestDetailResponse create(RequestCreateDto dto) {
        Center center = currentUser.center();
        if (center.getStatus() != CenterStatus.APPROVED) {
            throw new ForbiddenException("حساب مرکز شما غیرفعال است");
        }

        Request request = Request.builder()
                .center(center)
                .category(allowedCategory(center, dto.categoryId()))
                .city(loadCity(dto.cityId()))
                .title(dto.title())
                .description(dto.description())
                .amountNeeded(dto.amountNeeded())
                .deadline(dto.deadline())
                .imageUrl(dto.imageUrl())
                .contactInfo(dto.contactInfo())
                .details(dto.details() == null ? Map.of() : dto.details())
                .status(dto.submit() ? RequestStatus.PENDING : RequestStatus.DRAFT)
                .build();
        request.setUrgency(dto.urgency());

        // Saved once to obtain the identity that the public code -- and therefore the slug -- is
        // derived from, then updated in the same transaction.
        request = requests.save(request);
        request.setCode(buildCode(request.getId()));
        request.setSlug(SlugUtil.requestSlug(request.getTitle(), request.getCode()));

        return mapper.toDetail(requests.save(request), 0, true);
    }

    @Transactional
    public RequestDetailResponse updateByCenter(Long id, RequestUpdateDto dto) {
        Request request = ownedByCurrentCenter(id);
        if (!statusPolicy.isEditableByCenter(request.getStatus())) {
            throw new ConflictException("NOT_EDITABLE", "درخواست تکمیل‌شده قابل ویرایش نیست");
        }
        applyUpdate(request, dto, allowedCategory(request.getCenter(), dto.categoryId()));
        return mapper.toDetail(requests.save(request), 0, true);
    }

    @Transactional
    public RequestDetailResponse updateByAdmin(Long id, RequestUpdateDto dto) {
        Request request = loadById(id);
        applyUpdate(request, dto, allowedCategory(request.getCenter(), dto.categoryId()));
        request.setMetaTitle(dto.metaTitle());
        request.setMetaDescription(dto.metaDescription());
        return mapper.toDetail(requests.save(request), 0, true);
    }

    /** «ارسال برای بررسی» -- moves a draft or rejected request into the admin queue. */
    @Transactional
    public RequestDetailResponse submitForReview(Long id) {
        Request request = ownedByCurrentCenter(id);
        statusPolicy.assertTransition(request.getStatus(), RequestStatus.PENDING);
        request.setStatus(RequestStatus.PENDING);
        request.setStatusNote(null);
        return mapper.toDetail(requests.save(request), 0, true);
    }

    @Transactional
    public RequestDetailResponse changeStatus(Long id, RequestStatusChangeDto dto) {
        Request request = loadById(id);
        RequestStatus target = dto.status();

        statusPolicy.assertTransition(request.getStatus(), target);
        statusPolicy.assertNoteProvided(target, dto.note());

        boolean firstPublication = target == RequestStatus.PUBLISHED && request.getPublishedAt() == null;

        request.setStatus(target);
        request.setStatusNote(dto.note());
        if (firstPublication) {
            request.setPublishedAt(LocalDateTime.now(urls.zone()));
        }
        Request saved = requests.save(request);

        if (firstPublication) {
            // Announced after the transaction commits, not inside it. Publishing used to run
            // synchronously within the create transaction against an HTTP client with no timeouts,
            // so a slow messaging API held a database transaction open indefinitely.
            events.publishEvent(new RequestPublishedEvent(saved.getId()));
        }
        return mapper.toDetail(saved, 0, true);
    }

    @Transactional
    public RequestDetailResponse markCompletedByCenter(Long id) {
        Request request = ownedByCurrentCenter(id);
        statusPolicy.assertTransition(request.getStatus(), RequestStatus.COMPLETED);
        request.setStatus(RequestStatus.COMPLETED);
        return mapper.toDetail(requests.save(request), 0, true);
    }

    @Transactional
    public void softDeleteByCenter(Long id) {
        softDelete(ownedByCurrentCenter(id));
    }

    @Transactional
    public void softDeleteByAdmin(Long id) {
        softDelete(loadById(id));
    }

    @Transactional
    public RequestDetailResponse addDocuments(Long id, List<String> filenames) {
        Request request = ownedByCurrentCenter(id);
        List<String> documents = new ArrayList<>(request.getDocuments());
        documents.addAll(filenames);
        request.setDocuments(documents);
        return mapper.toDetail(requests.save(request), 0, true);
    }

    /** Used when an admin deactivates a whole centre. */
    @Transactional
    public void deactivateAllForCenter(Long centerId) {
        Specification<Request> spec = Specification.allOf(
                notDeleted(),
                centerIdEquals(centerId),
                statusIn(List.of(RequestStatus.PUBLISHED, RequestStatus.PENDING, RequestStatus.DRAFT)));
        requests.findAll(spec, Pageable.unpaged()).forEach(request -> {
            request.setStatus(RequestStatus.INACTIVE);
            requests.save(request);
        });
    }

    // ------------------------------------------------------------------ internals

    private Specification<Request> publicSpec(RequestFilter filter, Collection<RequestStatus> statuses) {
        return Specification.allOf(
                notDeleted(),
                statusIn(statuses),
                categoryIdIn(filter.categoryIds()),
                categorySlugIn(filter.categorySlugs()),
                urgencyIn(filter.urgencies()),
                cityIdIn(filter.cityIds()),
                cityNameIn(filter.cityNames()),
                provinceIdEquals(filter.provinceId()),
                centerIdEquals(filter.centerId()),
                centerSlugEquals(filter.centerSlug()),
                textMatches(filter.query()));
    }

    /**
     * Visitors may ask for PUBLISHED or COMPLETED. The default is PUBLISHED alone, so a completed
     * request leaves the active listing while keeping a working URL.
     */
    private Collection<RequestStatus> resolvePublicStatuses(List<RequestStatus> requested) {
        if (requested == null || requested.isEmpty()) {
            return RequestStatus.PUBLIC_DEFAULT;
        }
        List<RequestStatus> allowed = requested.stream()
                .filter(RequestStatus.PUBLICLY_REQUESTABLE::contains)
                .toList();
        if (allowed.isEmpty()) {
            throw new ValidationException("وضعیت درخواستی قابل نمایش نیست");
        }
        return allowed;
    }

    private void applyUpdate(Request request, RequestUpdateDto dto, Category category) {
        request.setTitle(dto.title());
        request.setCategory(category);
        request.setCity(loadCity(dto.cityId()));
        request.setDescription(dto.description());
        request.setAmountNeeded(dto.amountNeeded());
        request.setDeadline(dto.deadline());
        request.setImageUrl(dto.imageUrl());
        request.setContactInfo(dto.contactInfo());
        if (dto.urgency() != null) {
            request.setUrgency(dto.urgency());
        }
        if (dto.details() != null) {
            request.setDetails(dto.details());
        }
        // A slug only tracks the title until first publication; afterwards it is frozen, because
        // changing a live URL without a redirect discards whatever ranking it has earned.
        if (!request.isSlugFrozen()) {
            request.setSlug(SlugUtil.requestSlug(request.getTitle(), request.getCode()));
        }
    }

    /**
     * Deletion is soft. The row survives so the URL can answer 410 rather than 404, and so an
     * accidental deletion is recoverable.
     */
    private void softDelete(Request request) {
        if (request.isDeleted()) {
            return;
        }
        request.setDeletedAt(LocalDateTime.now(urls.zone()));
        requests.save(request);
    }

    private Request loadById(Long id) {
        return requests.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new NotFoundException("درخواست یافت نشد"));
    }

    private Request ownedByCurrentCenter(Long id) {
        Center center = currentUser.center();
        Request request = loadById(id);
        if (request.getCenter() == null || !request.getCenter().getId().equals(center.getId())) {
            throw new ForbiddenException("این درخواست متعلق به مرکز شما نیست");
        }
        return request;
    }

    /** A centre may only publish in the categories an admin granted it. */
    private Category allowedCategory(Center center, Long categoryId) {
        Category category = categories.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("دسته‌بندی یافت نشد"));
        boolean permitted = center != null
                && center.getCategories() != null
                && center.getCategories().stream().anyMatch(c -> c.getId().equals(category.getId()));
        if (!permitted) {
            throw new ValidationException("این دسته‌بندی در فهرست دسته‌های مجاز مرکز نیست");
        }
        return category;
    }

    private City loadCity(Long cityId) {
        if (cityId == null) {
            return null;
        }
        return cities.findById(cityId).orElseThrow(() -> new NotFoundException("شهر یافت نشد"));
    }

    private RuntimeException notFoundOrGone(String slug) {
        // The row is kept on delete precisely so this distinction can be made.
        if (requests.findBySlug(slug).isPresent()) {
            return new GoneException("این درخواست حذف شده است");
        }
        return new NotFoundException("درخواست یافت نشد");
    }

    private Map<RequestStatus, Long> toStatusMap(List<RequestRepository.StatusCount> counts) {
        Map<RequestStatus, Long> result = new EnumMap<>(RequestStatus.class);
        for (RequestStatus status : RequestStatus.values()) {
            result.put(status, 0L);
        }
        counts.forEach(count -> result.put(count.getStatus(), count.getTotal()));
        return result;
    }

    private static String buildCode(Long id) {
        return "RQ-" + (1000 + id);
    }

    /** Kept for the messaging listener, which needs the entity rather than a DTO. */
    @Transactional(readOnly = true)
    public Request loadForMessaging(Long id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("درخواست یافت نشد"));
    }

    @Transactional
    public void recordMessagingResult(Long id, String channel, Integer messageId) {
        requests.findById(id).ifPresent(request -> {
            if ("telegram".equals(channel)) {
                request.setTelegramPosted(true);
                request.setTelegramMessageId(messageId);
            } else if ("bale".equals(channel)) {
                request.setBalePosted(true);
                request.setBaleMessageId(messageId);
            }
            requests.save(request);
        });
    }

    /** Sitemap feed: only rows that are actually indexable. */
    @Transactional(readOnly = true)
    public Page<Request> indexable(Pageable pageable) {
        return requests.findIndexable(RequestStatus.PUBLICLY_REQUESTABLE, pageable);
    }
}
