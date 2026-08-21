package com.charity.app.service;

import com.charity.app.common.SlugUtil;
import com.charity.app.common.error.ConflictException;
import com.charity.app.common.error.NotFoundException;
import com.charity.app.common.error.ValidationException;
import com.charity.app.mapper.DocumentMapper;
import com.charity.app.model.DocumentCategory;
import com.charity.app.model.enums.DocumentScope;
import com.charity.app.payload.DocumentCategoryRequest;
import com.charity.app.payload.DocumentCategoryResponse;
import com.charity.app.repository.CenterDocumentRepository;
import com.charity.app.repository.DocumentCategoryRepository;
import com.charity.app.repository.RequestDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin CRUD for the two document taxonomies, modelled on {@link CategoryService}.
 *
 * <p>Everything is scoped. A name or slug is unique within its scope, not globally, so «صورت مالی»
 * can exist once for requests and once for centres without either list having to invent a
 * disambiguating name.
 */
@Service
@RequiredArgsConstructor
public class DocumentCategoryService {

    private final DocumentCategoryRepository categories;
    private final RequestDocumentRepository requestDocuments;
    private final CenterDocumentRepository centerDocuments;
    private final DocumentMapper mapper;

    @Transactional(readOnly = true)
    public List<DocumentCategoryResponse> listByScope(DocumentScope scope) {
        return categories.findByScopeOrderBySortOrderAscNameAsc(scope).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /** What the upload pickers and the public endpoint see: active rows only. */
    @Transactional(readOnly = true)
    public List<DocumentCategoryResponse> listActiveByScope(DocumentScope scope) {
        return categories.findByScopeAndActiveTrueOrderBySortOrderAscNameAsc(scope).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public DocumentCategoryResponse create(DocumentCategoryRequest req) {
        if (categories.existsByScopeAndName(req.scope(), req.name())) {
            throw new ConflictException("NAME_TAKEN", "دسته‌بندی دیگری با این نام در این فهرست وجود دارد");
        }
        DocumentCategory category = DocumentCategory.builder()
                .scope(req.scope())
                .name(req.name())
                .slug(resolveSlug(req, null))
                .description(blankToNull(req.description()))
                .sortOrder(req.sortOrder() == null ? 0 : req.sortOrder())
                .active(req.active())
                .build();
        return mapper.toResponse(categories.save(category));
    }

    /**
     * The scope of an existing category is not changed here even if the body carries a different
     * one: documents are already filed under it, and moving the category across would silently
     * strand every one of them on the wrong side.
     */
    @Transactional
    public DocumentCategoryResponse update(Long id, DocumentCategoryRequest req) {
        DocumentCategory category = load(id);

        if (categories.existsByScopeAndNameAndIdNot(category.getScope(), req.name(), id)) {
            throw new ConflictException("NAME_TAKEN", "دسته‌بندی دیگری با این نام در این فهرست وجود دارد");
        }
        category.setName(req.name());
        category.setSlug(resolveSlug(new DocumentCategoryRequest(
                category.getScope(), req.name(), req.slug(), req.description(),
                req.sortOrder(), req.active()), id));
        category.setDescription(blankToNull(req.description()));
        if (req.sortOrder() != null) {
            category.setSortOrder(req.sortOrder());
        }
        category.setActive(req.active());

        return mapper.toResponse(categories.save(category));
    }

    /**
     * Deleting a category in use requires nominating a replacement, exactly as the need taxonomy
     * does -- a bare delete would fail on the foreign key and surface as a 500 with SQL in the body.
     *
     * <p>Deactivating is almost always the better move and is what the admin page recommends: an
     * inactive category leaves every upload picker while the documents already filed under it keep
     * rendering with its name. Deleting one rewrites history for documents nobody asked about.
     *
     * @param replacementId category to move dependent documents to; required when any exist, and it
     *                      must share this category's scope
     */
    @Transactional
    public void delete(Long id, Long replacementId) {
        DocumentCategory target = load(id);

        // Counted on both tables rather than only the one the scope implies. The scope assertion in
        // DocumentService is what keeps them apart, and a foreign key does not care which service
        // wrote the row -- if anything ever slipped through, this is where it must not be ignored.
        long dependents = requestDocuments.countByCategoryId(id) + centerDocuments.countByCategoryId(id);
        if (dependents > 0) {
            if (replacementId == null || replacementId.equals(id)) {
                throw new ConflictException("DOCUMENT_CATEGORY_IN_USE",
                        ("این دسته‌بندی در %d مدرک استفاده شده است. "
                                + "برای حذف، دسته‌بندی جایگزین را انتخاب کنید.").formatted(dependents));
            }
            DocumentCategory replacement = categories.findById(replacementId)
                    .orElseThrow(() -> new NotFoundException("دسته‌بندی جایگزین یافت نشد"));
            if (replacement.getScope() != target.getScope()) {
                throw new ValidationException("دسته‌بندی جایگزین باید از همان فهرست باشد");
            }
            // Never null out category_id: a document with no category cannot be grouped or shown.
            requestDocuments.reassignCategory(id, replacementId);
            centerDocuments.reassignCategory(id, replacementId);
        }
        categories.delete(target);
    }

    private DocumentCategory load(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new NotFoundException("دسته‌بندی مدارک یافت نشد"));
    }

    /** Falls back to deriving the slug from the name when the admin leaves the field blank. */
    private String resolveSlug(DocumentCategoryRequest req, Long currentId) {
        String slug = blankToNull(req.slug());
        if (slug == null) {
            slug = SlugUtil.slugify(req.name(), 120);
        }
        if (slug.isBlank()) {
            throw new ValidationException("امکان ساخت نشانی یکتا از این نام وجود ندارد؛ آن را دستی وارد کنید");
        }
        boolean taken = currentId == null
                ? categories.existsByScopeAndSlug(req.scope(), slug)
                : categories.existsByScopeAndSlugAndIdNot(req.scope(), slug, currentId);
        if (taken) {
            throw new ConflictException("SLUG_TAKEN", "دسته‌بندی دیگری با این نشانی یکتا در این فهرست وجود دارد");
        }
        return slug;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
