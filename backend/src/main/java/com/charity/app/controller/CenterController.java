package com.charity.app.controller;

import com.charity.app.common.Paging;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.Urgency;
import com.charity.app.payload.*;
import com.charity.app.service.CenterService;
import com.charity.app.service.DocumentService;
import com.charity.app.service.FileStorageService;
import com.charity.app.service.RequestAnnouncementService;
import com.charity.app.service.RequestService;
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
@RequestMapping("/api/center")
@PreAuthorize("hasRole('CENTER')")
@RequiredArgsConstructor
public class CenterController {

    private final CenterService centers;
    private final RequestService requests;
    private final RequestAnnouncementService announcements;
    private final DocumentService documents;
    private final FileStorageService storage;

    // ---------------------------------------------------------------- profile

    @GetMapping("/me")
    public CenterResponse me() {
        return centers.currentCenter();
    }

    @PutMapping("/me")
    public CenterResponse updateMe(@Valid @RequestBody UpdateCenterProfileRequest request) {
        return centers.updateOwnProfile(request);
    }

    @PostMapping("/me/logo")
    public CenterResponse uploadLogo(@RequestParam("file") MultipartFile file) {
        return centers.setOwnLogo(storage.store(file));
    }

    @PutMapping("/me/logo")
    public CenterResponse setLogo(@Valid @RequestBody SetLogoDto request) {
        return centers.setOwnLogo(request.logoUrl());
    }

    // ---------------------------------------------------------------- requests

    @GetMapping("/requests")
    public Page<RequestSummary> listRequests(@RequestParam(required = false) List<RequestStatus> status,
                                             @RequestParam(required = false) List<Long> categoryId,
                                             @RequestParam(required = false) List<Urgency> urgency,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size,
                                             @RequestParam(required = false) String sort) {
        RequestFilter filter = new RequestFilter(
                categoryId, null, urgency, null, null, null, null, null, status, q);
        return requests.centerList(filter, Paging.of(page, size, sort == null ? "newest" : sort));
    }

    /** Per-status totals for the panel's four stat cards. */
    @GetMapping("/requests/stats")
    public Map<RequestStatus, Long> stats() {
        return requests.centerStats();
    }

    @GetMapping("/requests/{id}")
    public RequestDetailResponse getRequest(@PathVariable Long id) {
        return requests.centerDetail(id);
    }

    /** {@code submit=false} saves a draft; {@code submit=true} sends it for admin review. */
    @PostMapping("/requests")
    public RequestDetailResponse createRequest(@Valid @RequestBody RequestCreateDto request) {
        return requests.create(request);
    }

    @PutMapping("/requests/{id}")
    public RequestDetailResponse updateRequest(@PathVariable Long id,
                                               @Valid @RequestBody RequestUpdateDto request) {
        return requests.updateByCenter(id, request);
    }

    /** Publishes a draft. The path is kept so existing panel builds keep working. */
    @PostMapping("/requests/{id}/submit")
    public RequestDetailResponse publishRequest(@PathVariable Long id) {
        return requests.publishByCenter(id);
    }

    @PostMapping("/requests/{id}/complete")
    public RequestDetailResponse completeRequest(@PathVariable Long id) {
        return requests.markCompletedByCenter(id);
    }

    /**
     * A centre changing the status of its own request -- the mirror of the admin endpoint.
     *
     * <p>The two fixed-target verbs above stay for the panel builds that already call them; this is
     * the one the status dialog uses, so withdrawing a request no longer means deleting it. The
     * transitions themselves are policed by {@code RequestStatusPolicy}, ownership by the service,
     * and a request an admin took down is refused there.
     */
    @PostMapping("/requests/{id}/status")
    public RequestDetailResponse changeRequestStatus(@PathVariable Long id,
                                                     @Valid @RequestBody RequestStatusChangeDto request) {
        return requests.changeStatusByCenter(id, request);
    }

    @DeleteMapping("/requests/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        requests.softDeleteByCenter(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * The mirror of the admin endpoint, for a centre's own request only.
     *
     * <p>Ownership is asserted in the service, not here -- the id comes from the client and a
     * centre listing its own requests is no guarantee of what it will POST.
     */
    @PostMapping("/requests/{id}/announce")
    public RequestDetailResponse announceRequest(@PathVariable Long id) {
        return announcements.reannounce(id, true);
    }

    /**
     * Attaches documents to one of this centre's requests.
     *
     * <p>Allowed in every status, {@code COMPLETED} and an admin takedown included -- see
     * {@link com.charity.app.service.DocumentService}. Nothing here consults
     * {@code RequestStatusPolicy}: attaching a document is not a status transition.
     *
     * @param title honoured only when exactly one file is posted
     */
    @PostMapping("/requests/{id}/documents")
    public RequestDetailResponse uploadRequestDocuments(@PathVariable Long id,
                                                        @RequestParam("files") List<MultipartFile> files,
                                                        @RequestParam("categoryId") Long categoryId,
                                                        @RequestParam(value = "title", required = false) String title) {
        return documents.uploadToRequest(id, categoryId, title, files);
    }

    /** Refused with 409 while an admin's takedown is in force; allowed in every other status. */
    @DeleteMapping("/requests/{requestId}/documents/{documentId}")
    public ResponseEntity<Void> deleteRequestDocument(@PathVariable Long requestId,
                                                      @PathVariable Long documentId) {
        documents.deleteRequestDocumentByCenter(requestId, documentId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- own documents

    /** The centre's own paperwork -- licence, articles, accounts. Public on its centre page. */
    @PostMapping("/me/documents")
    public CenterResponse uploadOwnDocuments(@RequestParam("files") List<MultipartFile> files,
                                             @RequestParam("categoryId") Long categoryId,
                                             @RequestParam(value = "title", required = false) String title) {
        return documents.uploadToOwnCenter(categoryId, title, files);
    }

    @DeleteMapping("/me/documents/{documentId}")
    public ResponseEntity<Void> deleteOwnDocument(@PathVariable Long documentId) {
        documents.deleteOwnCenterDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
