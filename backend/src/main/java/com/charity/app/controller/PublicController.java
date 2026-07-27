package com.charity.app.controller;

import com.charity.app.model.Category;
import com.charity.app.model.City;
import com.charity.app.model.Center;
import com.charity.app.model.Notice;
import com.charity.app.model.Province;
import com.charity.app.payload.CenterResponse;
import com.charity.app.payload.CharityCaseResponse;
import com.charity.app.service.CategoryService;
import com.charity.app.service.CenterService;
import com.charity.app.service.CharityCaseService;
import com.charity.app.service.CityService;
import com.charity.app.service.FileStorageService;
import com.charity.app.service.NoticeService;
import com.charity.app.service.ProvinceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final CharityCaseService caseService;
    private final CategoryService categoryService;
    private final NoticeService noticeService;
    private final CenterService centerService;
    private final FileStorageService fileStorageService;
    private final ProvinceService provinceService;
    private final CityService cityService;

    @GetMapping("/cases")
    public ResponseEntity<Page<CharityCaseResponse>> listCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long provinceId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long centerId,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(caseService.publicList(
                pageable, q, categoryId, provinceId, cityId, centerId, status));
    }

    @GetMapping("/cases/{id}")
    public ResponseEntity<CharityCaseResponse> getCase(@PathVariable Long id) {
        return ResponseEntity.ok(caseService.getVisible(id));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> categories() {
        return ResponseEntity.ok(categoryService.listActive());
    }

    @GetMapping("/notices")
    public ResponseEntity<Map<String, Object>> notices() {
        List<Notice> footer = noticeService.listActiveByPosition(Notice.Position.FOOTER);
        List<Notice> banner = noticeService.listActiveByPosition(Notice.Position.BANNER);
        return ResponseEntity.ok(Map.of("footer", footer, "banner", banner));
    }

    @GetMapping("/centers/{id}")
    public ResponseEntity<?> centerProfile(@PathVariable Long id) {
        return ResponseEntity.ok(centerService.getPublicProfile(id));
    }

    @GetMapping("/centers")
    public ResponseEntity<Page<CenterResponse>> listCenters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(centerService.listByStatus(Center.Status.APPROVED, pageable));
    }

    @GetMapping("/provinces")
    public ResponseEntity<List<Province>> provinces(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(provinceService.list(q));
    }

    @GetMapping("/cities")
    public ResponseEntity<List<City>> cities(@RequestParam(required = false) Long provinceId,
                                             @RequestParam(required = false) String q) {
        return ResponseEntity.ok(cityService.list(provinceId, q));
    }

    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        try {
            Path path = fileStorageService.resolve(filename);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
