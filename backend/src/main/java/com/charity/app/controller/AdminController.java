package com.charity.app.controller;

import com.charity.app.common.Paging;
import com.charity.app.model.Province;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.Urgency;
import com.charity.app.payload.*;
import com.charity.app.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final RequestService requests;
    private final RequestAnnouncementService announcements;
    private final CenterService centers;
    private final CategoryService categories;
    private final NoticeService notices;
    private final ProvinceService provinces;
    private final CityService cities;
    private final UserService users;

    // ---------------------------------------------------------------- requests

    @GetMapping("/requests")
    public Page<RequestSummary> listRequests(@RequestParam(required = false) List<RequestStatus> status,
                                             @RequestParam(required = false) List<Long> categoryId,
                                             @RequestParam(required = false) List<Urgency> urgency,
                                             @RequestParam(required = false) List<Long> cityId,
                                             @RequestParam(required = false) Long centerId,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size,
                                             @RequestParam(required = false) String sort) {
        RequestFilter filter = new RequestFilter(
                categoryId, null, urgency, cityId, null, null, centerId, null, status, q);
        return requests.adminList(filter, Paging.of(page, size, sort == null ? "newest" : sort));
    }

    /** The five stat cards. Every status is present even at zero. */
    @GetMapping("/requests/stats")
    public Map<RequestStatus, Long> requestStats() {
        return requests.adminStats();
    }

    @GetMapping("/requests/{id}")
    public RequestDetailResponse getRequest(@PathVariable Long id) {
        return requests.adminDetail(id);
    }

    @PutMapping("/requests/{id}")
    public RequestDetailResponse updateRequest(@PathVariable Long id,
                                               @Valid @RequestBody RequestUpdateDto request) {
        return requests.updateByAdmin(id, request);
    }

    /**
     * The single status-change endpoint behind the admin panel's «تغییر وضعیت درخواست» card.
     * Admins no longer approve requests -- centres publish their own -- so what is left here is
     * taking one down, putting it back up, or marking it met. Deactivating without a note is refused.
     */
    @PostMapping("/requests/{id}/status")
    public RequestDetailResponse changeStatus(@PathVariable Long id,
                                              @Valid @RequestBody RequestStatusChangeDto request) {
        return requests.changeStatus(id, request);
    }

    @DeleteMapping("/requests/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        requests.softDeleteByAdmin(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Sends a published request to the messaging channels that do not already carry it.
     *
     * <p>The automatic announcement fires once, on first publication, so a channel that was down at
     * that moment stayed missing for good. Answers synchronously -- see
     * {@link com.charity.app.service.RequestAnnouncementService#reannounce}.
     */
    @PostMapping("/requests/{id}/announce")
    public RequestDetailResponse announceRequest(@PathVariable Long id) {
        return announcements.reannounce(id, false);
    }

    // ---------------------------------------------------------------- centres

    @GetMapping("/centers")
    public Page<CenterResponse> listCenters(@RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        return centers.adminList(Paging.of(page, size, org.springframework.data.domain.Sort.by("name")));
    }

    @GetMapping("/centers/{id}")
    public CenterResponse getCenter(@PathVariable Long id) {
        return centers.adminGet(id);
    }

    @PostMapping("/centers")
    public CenterResponse createCenter(@Valid @RequestBody CreateCenterByAdminRequest request) {
        return centers.createByAdmin(request);
    }

    @PutMapping("/centers/{id}")
    public CenterResponse updateCenter(@PathVariable Long id,
                                       @Valid @RequestBody UpdateCenterByAdminRequest request) {
        return centers.updateByAdmin(id, request);
    }

    @PutMapping("/centers/{id}/categories")
    public CenterResponse setCenterCategories(@PathVariable Long id,
                                              @Valid @RequestBody SetCategoriesDto request) {
        return centers.setCategories(id, request);
    }

    @PostMapping("/centers/{id}/activate")
    public CenterResponse activateCenter(@PathVariable Long id) {
        return centers.setActive(id, true);
    }

    /** Deactivating a centre also withdraws its live requests, so no dead contact is left listed. */
    @PostMapping("/centers/{id}/deactivate")
    public CenterResponse deactivateCenter(@PathVariable Long id) {
        return centers.setActive(id, false);
    }

    @DeleteMapping("/centers/{id}")
    public ResponseEntity<Void> deleteCenter(@PathVariable Long id) {
        centers.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- categories

    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return categories.listAll();
    }

    @PostMapping("/categories")
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return categories.create(request);
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id,
                                           @Valid @RequestBody CategoryRequest request) {
        return categories.update(id, request);
    }

    /**
     * @param replacementId required when any request still uses this category; those requests are
     *                      moved across rather than being orphaned or blocking the delete
     */
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id,
                                               @RequestParam(required = false) Long replacementId) {
        categories.delete(id, replacementId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- announcements

    @GetMapping("/notices")
    public List<NoticeResponse> listNotices() {
        return notices.listAll();
    }

    @GetMapping("/notices/{id}")
    public NoticeResponse getNotice(@PathVariable Long id) {
        return notices.get(id);
    }

    @PostMapping("/notices")
    public NoticeResponse createNotice(@Valid @RequestBody NoticeRequest request) {
        return notices.create(request);
    }

    @PutMapping("/notices/{id}")
    public NoticeResponse updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
        return notices.update(id, request);
    }

    @DeleteMapping("/notices/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long id) {
        notices.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- locations

    @GetMapping("/provinces")
    public List<Province> listProvinces(@RequestParam(required = false) String q) {
        return provinces.list(q);
    }

    @PostMapping("/provinces")
    public Province createProvince(@Valid @RequestBody NameRequest request) {
        return provinces.create(request);
    }

    @PutMapping("/provinces/{id}")
    public Province updateProvince(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return provinces.update(id, request);
    }

    @DeleteMapping("/provinces/{id}")
    public ResponseEntity<Void> deleteProvince(@PathVariable Long id) {
        provinces.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cities")
    public List<CityRef> listCities(@RequestParam(required = false) Long provinceId,
                                    @RequestParam(required = false) String q) {
        return cities.list(provinceId, q);
    }

    @PostMapping("/cities")
    public CityRef createCity(@RequestParam Long provinceId, @Valid @RequestBody NameRequest request) {
        return cities.create(provinceId, request);
    }

    @PutMapping("/cities/{id}")
    public CityRef updateCity(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return cities.update(id, request);
    }

    @DeleteMapping("/cities/{id}")
    public ResponseEntity<Void> deleteCity(@PathVariable Long id) {
        cities.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- own account

    @GetMapping("/me")
    public UserProfileResponse me() {
        return users.currentProfile();
    }

    @PutMapping("/me")
    public UserProfileResponse updateMe(@Valid @RequestBody UpdateAdminProfileRequest request) {
        return users.updateProfile(request);
    }
}
