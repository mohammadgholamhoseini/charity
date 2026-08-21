package com.charity.app.controller;

import com.charity.app.common.Paging;
import com.charity.app.model.Province;
import com.charity.app.model.enums.DocumentScope;
import com.charity.app.model.enums.NoticePlacement;
import com.charity.app.payload.*;
import com.charity.app.service.CategoryService;
import com.charity.app.service.CenterService;
import com.charity.app.service.CityService;
import com.charity.app.service.DocumentCategoryService;
import com.charity.app.service.NoticeService;
import com.charity.app.service.ProvinceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final CategoryService categories;
    private final DocumentCategoryService documentCategories;
    private final CenterService centers;
    private final ProvinceService provinces;
    private final CityService cities;
    private final NoticeService notices;

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> categories() {
        return cached(categories.listActive(), Duration.ofMinutes(10));
    }

    @GetMapping("/categories/{slug}")
    public ResponseEntity<CategoryResponse> category(@PathVariable String slug) {
        return cached(categories.getBySlug(slug), Duration.ofMinutes(10));
    }

    /**
     * The active document categories of one list, for the upload pickers in both panels.
     *
     * <p>Public rather than duplicated behind {@code /admin} and {@code /center}: the values are
     * already visible on every request and centre page that renders a document, and a centre needs
     * this list to upload at all. {@code GET /api/public/**} is {@code permitAll}, so no security
     * change goes with it.
     */
    @GetMapping("/document-categories")
    public ResponseEntity<List<DocumentCategoryResponse>> documentCategories(
            @RequestParam DocumentScope scope) {
        return cached(documentCategories.listActiveByScope(scope), Duration.ofMinutes(10));
    }

    @GetMapping("/centers")
    public ResponseEntity<Page<CenterCard>> centers(@RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size) {
        Page<CenterCard> result = centers.publicList(
                Paging.of(page, size, org.springframework.data.domain.Sort.by("name")));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result);
    }

    @GetMapping("/centers/{slug}")
    public ResponseEntity<CenterPublicProfile> center(@PathVariable String slug) {
        return cached(centers.publicProfile(slug), Duration.ofMinutes(10));
    }

    @GetMapping("/provinces")
    public ResponseEntity<List<Province>> provinces(@RequestParam(required = false) String q) {
        return cached(provinces.list(q), Duration.ofHours(1));
    }

    @GetMapping("/cities")
    public ResponseEntity<List<CityRef>> cities(@RequestParam(required = false) Long provinceId,
                                                @RequestParam(required = false) String q) {
        return cached(cities.list(provinceId, q), Duration.ofHours(1));
    }

    /**
     * The announcements currently on display, keyed by placement.
     *
     * <p>Returns at most one per placement -- choosing which announcement wins is the server's job,
     * not something the client should have to derive from a list.
     */
    @GetMapping("/announcements")
    public ResponseEntity<Map<String, NoticeResponse>> announcements() {
        Map<String, NoticeResponse> current = new HashMap<>();
        for (NoticePlacement placement : NoticePlacement.values()) {
            notices.currentFor(placement).ifPresent(notice -> current.put(placement.name(), notice));
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(2)).cachePublic())
                .body(current);
    }

    private <T> ResponseEntity<T> cached(T body, Duration maxAge) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(maxAge).cachePublic())
                .body(body);
    }
}
