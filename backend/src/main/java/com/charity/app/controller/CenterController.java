package com.charity.app.controller;

import com.charity.app.model.Center;
import com.charity.app.model.User;
import com.charity.app.payload.CreateCaseRequest;
import com.charity.app.payload.UpdateCaseRequest;
import com.charity.app.payload.CharityCaseResponse;
import com.charity.app.payload.UpdateCenterProfileRequest;
import com.charity.app.service.CenterService;
import com.charity.app.service.CharityCaseService;
import com.charity.app.service.FileStorageService;
import com.charity.app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/center")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CENTER')")
public class CenterController {

    private final CharityCaseService caseService;
    private final CenterService centerService;
    private final FileStorageService fileStorageService;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<Center> me() {
        return ResponseEntity.ok(centerService.currentCenter());
    }

    @PutMapping("/me")
    public ResponseEntity<Center> updateProfile(@Valid @RequestBody UpdateCenterProfileRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(centerService.updateOwnProfile(user, request));
    }

    @PostMapping("/cases")
    public ResponseEntity<CharityCaseResponse> createCase(@Valid @RequestBody CreateCaseRequest request) {
        return ResponseEntity.ok(caseService.createCase(request));
    }

    @GetMapping("/cases")
    public ResponseEntity<Page<CharityCaseResponse>> myCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(caseService.centerList(pageable));
    }

    @PostMapping("/cases/{id}/complete")
    public ResponseEntity<CharityCaseResponse> complete(@PathVariable Long id) {
        caseService.ensureOwnedByCurrentCenter(id);
        return ResponseEntity.ok(toResponse(caseService.markCompleted(id)));
    }

    @PutMapping("/cases/{id}")
    public ResponseEntity<CharityCaseResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateCaseRequest request) {
        caseService.ensureOwnedByCurrentCenter(id);
        return ResponseEntity.ok(caseService.updateCase(id, request));
    }

    @DeleteMapping("/cases/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        caseService.ensureOwnedByCurrentCenter(id);
        caseService.deleteCase(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cases/{id}/documents")
    public CharityCaseResponse uploadDocuments(@PathVariable Long id,
                                                               @RequestParam("files") List<MultipartFile> files) {
        caseService.ensureOwnedByCurrentCenter(id);
        List<String> saved = new ArrayList<>();
        for (MultipartFile f : files) {
            if (!f.isEmpty()) {
                saved.add(fileStorageService.store(f));
            }
        }
        caseService.addDocuments(id, saved);
        return caseService.getPublic(id);
    }

    @PostMapping("/me/logo")
    public ResponseEntity<Center> uploadLogo(@RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        String filename = fileStorageService.store(file);
        return ResponseEntity.ok(centerService.setLogo(user, filename));
    }

    @PutMapping("/me/logo")
    public ResponseEntity<Center> setLogoUrl(@RequestBody Map<String, String> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(centerService.setLogo(user, body.get("logoUrl")));
    }

    private CharityCaseResponse toResponse(com.charity.app.model.CharityCase c) {
        return caseService.getPublic(c.getId());
    }
}
