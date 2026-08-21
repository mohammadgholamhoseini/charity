package com.charity.app.controller;

import com.charity.app.common.Paging;
import com.charity.app.model.Province;
import com.charity.app.model.enums.DocumentScope;
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
import org.springframework.web.multipart.MultipartFile;

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
    private final DocumentService documents;
    private final DocumentCategoryService documentCategories;
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

    /**
     * Sets a new password for the centre's account, for a centre that lost it or locked itself out.
     * The admin types the password and passes it on out of band -- it is never echoed back here.
     */
    @PostMapping("/centers/{id}/password")
    public ResponseEntity<Void> resetCenterPassword(@PathVariable Long id,
                                                    @Valid @RequestBody ResetCenterPasswordRequest request) {
        centers.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/centers/{id}")
    public ResponseEntity<Void> deleteCenter(@PathVariable Long id) {
        centers.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- documents

    /**
     * Attaches documents to a centre. This is what the create-centre form calls once the centre
     * itself exists -- creation stays JSON, so the two arrive in two calls.
     *
     * @param title honoured only when exactly one file is posted
     */
    @PostMapping("/centers/{id}/documents")
    public CenterResponse uploadCenterDocuments(@PathVariable Long id,
                                                @RequestParam("files") List<MultipartFile> files,
                                                @RequestParam("categoryId") Long categoryId,
                                                @RequestParam(value = "title", required = false) String title) {
        return documents.uploadToCenter(id, categoryId, title, files);
    }

    @DeleteMapping("/centers/{centerId}/documents/{documentId}")
    public ResponseEntity<Void> deleteCenterDocument(@PathVariable Long centerId,
                                                     @PathVariable Long documentId) {
        documents.deleteCenterDocumentByAdmin(centerId, documentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Removes a document from any request, in any status. The moderation remedy for a scan that
     * names the beneficiary: the row and the file both go, which is why there is no admin upload
     * to match it -- request paperwork is the centre's to supply, and an admin's to take down.
     */
    @DeleteMapping("/requests/{requestId}/documents/{documentId}")
    public ResponseEntity<Void> deleteRequestDocument(@PathVariable Long requestId,
                                                      @PathVariable Long documentId) {
        documents.deleteRequestDocumentByAdmin(requestId, documentId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- document categories

    @GetMapping("/document-categories")
    public List<DocumentCategoryResponse> listDocumentCategories(@RequestParam DocumentScope scope) {
        return documentCategories.listByScope(scope);
    }

    @PostMapping("/document-categories")
    public DocumentCategoryResponse createDocumentCategory(
            @Valid @RequestBody DocumentCategoryRequest request) {
        return documentCategories.create(request);
    }

    @PutMapping("/document-categories/{id}")
    public DocumentCategoryResponse updateDocumentCategory(
            @PathVariable Long id, @Valid @RequestBody DocumentCategoryRequest request) {
        return documentCategories.update(id, request);
    }

    /**
     * Deactivating is nearly always the better move -- it clears the category out of every upload
     * picker while documents already filed under it keep rendering.
     *
     * @param replacementId required when any document still uses this category, and it must share
     *                      the category's scope; the documents are moved rather than orphaned
     */
    @DeleteMapping("/document-categories/{id}")
    public ResponseEntity<Void> deleteDocumentCategory(@PathVariable Long id,
                                                       @RequestParam(required = false) Long replacementId) {
        documentCategories.delete(id, replacementId);
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
