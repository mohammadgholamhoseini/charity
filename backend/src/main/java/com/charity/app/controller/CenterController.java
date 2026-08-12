package com.charity.app.controller;

import com.charity.app.common.Paging;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.Urgency;
import com.charity.app.payload.*;
import com.charity.app.service.CenterService;
import com.charity.app.service.FileStorageService;
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

    @PostMapping("/requests/{id}/submit")
    public RequestDetailResponse submitRequest(@PathVariable Long id) {
        return requests.submitForReview(id);
    }

    @PostMapping("/requests/{id}/complete")
    public RequestDetailResponse completeRequest(@PathVariable Long id) {
        return requests.markCompletedByCenter(id);
    }

    @DeleteMapping("/requests/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        requests.softDeleteByCenter(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{id}/documents")
    public RequestDetailResponse uploadDocuments(@PathVariable Long id,
                                                 @RequestParam("files") List<MultipartFile> files) {
        return requests.addDocuments(id, storage.storeAll(files));
    }
}
