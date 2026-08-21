package com.charity.app.service;

import com.charity.app.common.error.ConflictException;
import com.charity.app.common.error.ForbiddenException;
import com.charity.app.common.error.NotFoundException;
import com.charity.app.common.error.ValidationException;
import com.charity.app.mapper.CenterMapper;
import com.charity.app.mapper.RequestMapper;
import com.charity.app.model.Center;
import com.charity.app.model.CenterDocument;
import com.charity.app.model.DocumentCategory;
import com.charity.app.model.Request;
import com.charity.app.model.RequestDocument;
import com.charity.app.model.enums.DocumentScope;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.UserRole;
import com.charity.app.payload.CenterResponse;
import com.charity.app.payload.RequestDetailResponse;
import com.charity.app.repository.CenterDocumentRepository;
import com.charity.app.repository.CenterRepository;
import com.charity.app.repository.DocumentCategoryRepository;
import com.charity.app.repository.RequestDocumentRepository;
import com.charity.app.repository.RequestRepository;
import com.charity.app.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Attaching paperwork to a request or to a centre, and taking it off again.
 *
 * <p>Deliberately independent of {@link RequestService}. That class's write paths all run a status
 * check on the way in -- {@code isEditableByCenter} refuses a {@code COMPLETED} request,
 * {@code assertNotAdminTakedown} refuses one an admin has taken down -- and a document upload must
 * be allowed in both of those cases. A completed request may still need its receipts filed, and
 * supplying the documents an admin asked for is precisely how a centre answers a takedown: the
 * takedown exists to stop a centre <em>reversing</em> a moderation decision, not to stop it
 * responding to one. So this service does its own load, its own not-deleted filter and its own
 * ownership check, and touches no status field at all. Uploading is not a transition.
 *
 * <p>A soft-deleted request answers 404 here rather than 410. The 410 rule belongs to the public
 * URL; every panel path treats a deleted row as absent.
 *
 * <p>Deletion is hard -- the row and the file both. That is the privacy remedy, and a soft delete
 * that left a publicly-served scan on disk would not be one.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    /**
     * Per-owner ceiling. Twenty is well past what a genuine case needs and low enough that a
     * compromised centre account cannot turn the uploads directory into free file hosting.
     */
    private static final int MAX_DOCUMENTS_PER_OWNER = 20;

    /**
     * Per-call ceiling, and the reason no limit anywhere else has to move: 4 x 10 MB is exactly
     * {@code spring.servlet.multipart.max-request-size} and sits under nginx's 45 M, so a rejection
     * is Spring's Persian 413 rather than nginx's HTML one. The panel chunks a larger selection.
     */
    private static final int MAX_FILES_PER_CALL = 4;

    private static final int MAX_ORIGINAL_FILENAME = 255;
    private static final int MAX_CONTENT_TYPE = 120;

    private final RequestRepository requests;
    private final CenterRepository centers;
    private final DocumentCategoryRepository documentCategories;
    private final RequestDocumentRepository requestDocuments;
    private final CenterDocumentRepository centerDocuments;
    private final FileStorageService storage;
    private final CurrentUser currentUser;
    private final RequestMapper requestMapper;
    private final CenterMapper centerMapper;

    // ------------------------------------------------------------------ request documents

    /**
     * A centre attaching documents to one of its own requests, in any status.
     *
     * @param title honoured only when exactly one file is posted -- a label shared by four files
     *              describes none of them
     */
    @Transactional
    public RequestDetailResponse uploadToRequest(Long requestId, Long categoryId, String title,
                                                 List<MultipartFile> files) {
        Request request = ownedRequest(requestId);
        DocumentCategory category = category(categoryId, DocumentScope.REQUEST);
        assertBatch(files);

        int existing = request.getDocuments().size();
        assertCapacity(existing, files.size());

        List<String> stored = storeAll(files);
        UserRole role = currentUser.user().getRole();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            RequestDocument document = RequestDocument.builder()
                    .request(request)
                    .category(category)
                    .storedFilename(stored.get(i))
                    .originalFilename(displayName(file))
                    .title(files.size() == 1 ? blankToNull(title) : null)
                    .contentType(truncate(file.getContentType(), MAX_CONTENT_TYPE))
                    .sizeBytes(file.getSize())
                    .sortOrder(existing + i)
                    .uploadedByRole(role)
                    .build();
            request.getDocuments().add(document);
        }
        return requestMapper.toDetail(requests.save(request), 0, true);
    }

    /**
     * A centre removing a document from its own request.
     *
     * <p>The one refusal: while an admin's takedown is in force. Adding to such a request is how a
     * centre answers the moderation note; removing from it is how it would undo the evidence.
     */
    @Transactional
    public void deleteRequestDocumentByCenter(Long requestId, Long documentId) {
        Request request = ownedRequest(requestId);
        if (request.getStatus() == RequestStatus.INACTIVE
                && request.getDeactivatedBy() == UserRole.ADMIN) {
            throw new ConflictException("ADMIN_TAKEDOWN",
                    "این درخواست توسط ادمین غیرفعال شده است و حذف مدارک آن تنها از سوی ادمین ممکن است");
        }
        removeRequestDocument(request, documentId);
    }

    /** An admin removing any document from any request. The moderation remedy; never refused. */
    @Transactional
    public void deleteRequestDocumentByAdmin(Long requestId, Long documentId) {
        removeRequestDocument(loadRequest(requestId), documentId);
    }

    // ------------------------------------------------------------------ centre documents

    /** A centre attaching documents to its own profile. */
    @Transactional
    public CenterResponse uploadToOwnCenter(Long categoryId, String title, List<MultipartFile> files) {
        return uploadToCenter(currentUser.center(), categoryId, title, files);
    }

    /** An admin attaching documents to a centre -- what the create-centre form calls afterwards. */
    @Transactional
    public CenterResponse uploadToCenter(Long centerId, Long categoryId, String title,
                                         List<MultipartFile> files) {
        return uploadToCenter(loadCenter(centerId), categoryId, title, files);
    }

    @Transactional
    public void deleteOwnCenterDocument(Long documentId) {
        removeCenterDocument(currentUser.center(), documentId);
    }

    @Transactional
    public void deleteCenterDocumentByAdmin(Long centerId, Long documentId) {
        removeCenterDocument(loadCenter(centerId), documentId);
    }

    /**
     * Unlinks a stored file once nothing points at it any more.
     *
     * <p>Public because {@code CenterService.delete} needs the same guard: deleting a centre takes
     * its {@code center_documents} rows with it and the files have to follow, or the uploads
     * directory keeps every scan a deleted centre ever filed.
     */
    public void unlinkIfUnreferenced(String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) {
            return;
        }
        long referenced = requestDocuments.countByStoredFilename(storedFilename)
                + centerDocuments.countByStoredFilename(storedFilename);
        if (referenced == 0) {
            storage.delete(storedFilename);
        }
    }

    // ------------------------------------------------------------------ internals

    private CenterResponse uploadToCenter(Center center, Long categoryId, String title,
                                          List<MultipartFile> files) {
        DocumentCategory category = category(categoryId, DocumentScope.CENTER);
        assertBatch(files);

        int existing = center.getDocuments().size();
        assertCapacity(existing, files.size());

        List<String> stored = storeAll(files);
        UserRole role = currentUser.user().getRole();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            CenterDocument document = CenterDocument.builder()
                    .center(center)
                    .category(category)
                    .storedFilename(stored.get(i))
                    .originalFilename(displayName(file))
                    .title(files.size() == 1 ? blankToNull(title) : null)
                    .contentType(truncate(file.getContentType(), MAX_CONTENT_TYPE))
                    .sizeBytes(file.getSize())
                    .sortOrder(existing + i)
                    .uploadedByRole(role)
                    .build();
            center.getDocuments().add(document);
        }
        Center saved = centers.save(center);
        return centerMapper.toResponse(saved, activeRequestCount(saved.getId()));
    }

    /**
     * Removal goes through the owner's collection rather than a repository delete by id, so
     * {@code orphanRemoval} does the work and the entity in this persistence context does not keep
     * a row that is already gone. The file is unlinked after the flush, and only if no other row
     * still names it.
     */
    private void removeRequestDocument(Request request, Long documentId) {
        RequestDocument document = request.getDocuments().stream()
                .filter(d -> d.getId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("مدرک یافت نشد"));
        String stored = document.getStoredFilename();
        request.getDocuments().remove(document);
        requests.saveAndFlush(request);
        unlinkIfUnreferenced(stored);
    }

    private void removeCenterDocument(Center center, Long documentId) {
        CenterDocument document = center.getDocuments().stream()
                .filter(d -> d.getId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("مدرک یافت نشد"));
        String stored = document.getStoredFilename();
        center.getDocuments().remove(document);
        centers.saveAndFlush(center);
        unlinkIfUnreferenced(stored);
    }

    /** Not deleted, and belonging to the caller. No status is consulted -- see the class comment. */
    private Request ownedRequest(Long id) {
        Center center = currentUser.center();
        Request request = loadRequest(id);
        if (request.getCenter() == null || !request.getCenter().getId().equals(center.getId())) {
            throw new ForbiddenException("این درخواست متعلق به مرکز شما نیست");
        }
        return request;
    }

    private Request loadRequest(Long id) {
        return requests.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new NotFoundException("درخواست یافت نشد"));
    }

    private Center loadCenter(Long id) {
        return centers.findById(id).orElseThrow(() -> new NotFoundException("مرکز خیریه یافت نشد"));
    }

    /**
     * The category must exist, still be offered, and belong to the right list. The scope check is
     * what a second table would have enforced for free; it is the same shape as
     * {@code RequestService.allowedCategory}.
     */
    private DocumentCategory category(Long categoryId, DocumentScope scope) {
        if (categoryId == null) {
            throw new ValidationException("دسته‌بندی مدرک الزامی است");
        }
        DocumentCategory category = documentCategories.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("دسته‌بندی مدارک یافت نشد"));
        if (category.getScope() != scope) {
            throw new ValidationException("این دسته‌بندی برای این نوع مدرک نیست");
        }
        if (!category.isActive()) {
            throw new ValidationException("این دسته‌بندی غیرفعال است و نمی‌توان مدرک جدیدی در آن ثبت کرد");
        }
        return category;
    }

    /**
     * Writes the whole batch to disk before any row is built.
     *
     * <p>If the third of four files is a zip wearing a {@code .docx} extension, the transaction
     * rolls back and the first two rows never exist -- but the two files would already be on disk,
     * referenced by nothing and impossible to find again. They are unlinked here instead.
     */
    private List<String> storeAll(List<MultipartFile> files) {
        List<String> stored = new ArrayList<>(files.size());
        try {
            for (MultipartFile file : files) {
                stored.add(storage.store(file));
            }
        } catch (RuntimeException e) {
            stored.forEach(storage::delete);
            throw e;
        }
        return stored;
    }

    private void assertBatch(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ValidationException("فایلی برای بارگذاری انتخاب نشده است");
        }
        if (files.size() > MAX_FILES_PER_CALL) {
            throw new ValidationException(
                    "در هر بار می‌توانید حداکثر %d فایل بارگذاری کنید".formatted(MAX_FILES_PER_CALL));
        }
    }

    /** Checked before anything is written, so a refused batch leaves no orphan files behind. */
    private void assertCapacity(int existing, int incoming) {
        if (existing + incoming > MAX_DOCUMENTS_PER_OWNER) {
            throw new ConflictException("DOCUMENT_LIMIT",
                    "حداکثر %d مدرک قابل ثبت است؛ هم‌اکنون %d مدرک ثبت شده است."
                            .formatted(MAX_DOCUMENTS_PER_OWNER, existing));
        }
    }

    private long activeRequestCount(Long centerId) {
        return requests.countActiveByCenterIds(RequestStatus.PUBLISHED, List.of(centerId)).stream()
                .findFirst()
                .map(RequestRepository.CenterCount::getTotal)
                .orElse(0L);
    }

    /**
     * The uploader's own filename, for display only. Any directory part is dropped: it is never
     * used to build a path -- the stored name is a generated UUID -- but a browser rendering
     * {@code ../../etc/passwd} as a document title helps nobody.
     */
    private static String displayName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return null;
        }
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        return truncate(name.substring(slash + 1).trim(), MAX_ORIGINAL_FILENAME);
    }

    private static String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
