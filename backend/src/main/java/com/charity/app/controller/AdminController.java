package com.charity.app.controller;

import com.charity.app.model.City;
import com.charity.app.model.Province;
import com.charity.app.payload.CategoryRequest;
import com.charity.app.payload.CenterResponse;
import com.charity.app.payload.CreateCenterByAdminRequest;
import com.charity.app.payload.CharityCaseResponse;
import com.charity.app.payload.NoticeRequest;
import com.charity.app.payload.UpdateAdminProfileRequest;
import com.charity.app.payload.UpdateCaseRequest;
import com.charity.app.payload.UpdateCenterByAdminRequest;
import com.charity.app.payload.NameRequest;
import com.charity.app.service.CategoryService;
import com.charity.app.service.CenterService;
import com.charity.app.service.CharityCaseService;
import com.charity.app.service.CityService;
import com.charity.app.service.NoticeService;
import com.charity.app.service.ProvinceService;
import com.charity.app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final CharityCaseService caseService;
    private final CenterService centerService;
    private final CategoryService categoryService;
    private final NoticeService noticeService;
    private final ProvinceService provinceService;
    private final CityService cityService;
    private final UserService userService;

    @GetMapping("/cases")
    public ResponseEntity<Page<CharityCaseResponse>> listCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(caseService.adminList(pageable, status));
    }

    @PostMapping("/cases/{id}/complete")
    public ResponseEntity<CharityCaseResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(caseService.markCompleted(id)));
    }

    @PutMapping("/cases/{id}")
    public ResponseEntity<CharityCaseResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateCaseRequest request) {
        return ResponseEntity.ok(caseService.updateCase(id, request));
    }

    @DeleteMapping("/cases/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        caseService.deleteCase(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/centers/pending")
    public ResponseEntity<Page<CenterResponse>> pendingCenters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(centerService.listPendingResponse(pageable));
    }

    @GetMapping("/centers")
    public ResponseEntity<Page<CenterResponse>> allCenters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(centerService.listAllResponse(pageable));
    }

    @GetMapping("/centers/{id}")
    public ResponseEntity<CenterResponse> centerDetail(@PathVariable Long id) {
        return ResponseEntity.ok(centerService.getByIdResponse(id));
    }

    @PostMapping("/centers/{id}/approve")
    public ResponseEntity<CenterResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(toCenterResponse(centerService.approve(id)));
    }

    @PostMapping("/centers")
    public ResponseEntity<CenterResponse> createCenter(@Valid @RequestBody CreateCenterByAdminRequest request) {
        return ResponseEntity.ok(toCenterResponse(centerService.createByAdmin(request)));
    }

    @PutMapping("/centers/{id}")
    public ResponseEntity<CenterResponse> updateCenter(@PathVariable Long id,
                                               @Valid @RequestBody UpdateCenterByAdminRequest request) {
        return ResponseEntity.ok(toCenterResponse(centerService.updateByAdmin(id, request)));
    }

    @PostMapping("/centers/{id}/deactivate")
    public ResponseEntity<CenterResponse> deactivateCenter(@PathVariable Long id) {
        return ResponseEntity.ok(toCenterResponse(centerService.deactivate(id)));
    }

    @PostMapping("/centers/{id}/activate")
    public ResponseEntity<CenterResponse> activateCenter(@PathVariable Long id) {
        return ResponseEntity.ok(toCenterResponse(centerService.activate(id)));
    }

    @DeleteMapping("/centers/{id}")
    public ResponseEntity<Void> deleteCenter(@PathVariable Long id) {
        centerService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> adminProfile() {
        return ResponseEntity.ok(userService.currentProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateAdminProfile(@Valid @RequestBody UpdateAdminProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @GetMapping("/provinces")
    public ResponseEntity<List<Province>> listProvinces(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(provinceService.list(q));
    }

    @PostMapping("/provinces")
    public ResponseEntity<Province> createProvince(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.ok(provinceService.create(request));
    }

    @PutMapping("/provinces/{id}")
    public ResponseEntity<Province> updateProvince(@PathVariable Long id,
                                                   @Valid @RequestBody NameRequest request) {
        return ResponseEntity.ok(provinceService.update(id, request));
    }

    @DeleteMapping("/provinces/{id}")
    public ResponseEntity<Void> deleteProvince(@PathVariable Long id) {
        provinceService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cities")
    public ResponseEntity<List<City>> listCities(@RequestParam(required = false) Long provinceId,
                                                 @RequestParam(required = false) String q) {
        return ResponseEntity.ok(cityService.list(provinceId, q));
    }

    @PostMapping("/cities")
    public ResponseEntity<City> createCity(@RequestParam Long provinceId,
                                           @Valid @RequestBody NameRequest request) {
        return ResponseEntity.ok(cityService.create(provinceId, request));
    }

    @PutMapping("/cities/{id}")
    public ResponseEntity<City> updateCity(@PathVariable Long id,
                                           @Valid @RequestBody NameRequest request) {
        return ResponseEntity.ok(cityService.update(id, request));
    }

    @DeleteMapping("/cities/{id}")
    public ResponseEntity<Void> deleteCity(@PathVariable Long id) {
        cityService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/centers/{id}/categories")
    public ResponseEntity<CenterResponse> setCenterCategories(@PathVariable Long id,
                                                       @RequestBody List<Long> categoryIds) {
        return ResponseEntity.ok(toCenterResponse(centerService.setCategories(id, categoryIds)));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<com.charity.app.model.Category>> listCategories() {
        return ResponseEntity.ok(categoryService.listAll());
    }

    @PostMapping("/categories")
    public ResponseEntity<com.charity.app.model.Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.create(request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<com.charity.app.model.Category> updateCategory(@PathVariable Long id,
                                                                         @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/notices")
    public ResponseEntity<List<com.charity.app.model.Notice>> listNotices() {
        return ResponseEntity.ok(noticeService.listAll());
    }

    @PostMapping("/notices")
    public ResponseEntity<com.charity.app.model.Notice> createNotice(@Valid @RequestBody NoticeRequest request) {
        return ResponseEntity.ok(noticeService.create(request));
    }

    @PutMapping("/notices/{id}")
    public ResponseEntity<com.charity.app.model.Notice> updateNotice(@PathVariable Long id,
                                                                     @Valid @RequestBody NoticeRequest request) {
        return ResponseEntity.ok(noticeService.update(id, request));
    }

    @DeleteMapping("/notices/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long id) {
        noticeService.delete(id);
        return ResponseEntity.ok().build();
    }

    private CharityCaseResponse toResponse(com.charity.app.model.CharityCase c) {
        return caseService.getPublic(c.getId());
    }

    private CenterResponse toCenterResponse(com.charity.app.model.Center c) {
        return centerService.getByIdResponse(c.getId());
    }
}
