package com.charity.app.service;

import com.charity.app.common.SlugUtil;
import com.charity.app.common.error.ConflictException;
import com.charity.app.common.error.NotFoundException;
import com.charity.app.common.error.ValidationException;
import com.charity.app.mapper.CategoryMapper;
import com.charity.app.model.Category;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.payload.CategoryRequest;
import com.charity.app.payload.CategoryResponse;
import com.charity.app.repository.CategoryRepository;
import com.charity.app.repository.CenterRepository;
import com.charity.app.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categories;
    private final CenterRepository centers;
    private final RequestRepository requests;
    private final CategoryMapper mapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return withCounts(categories.findByActiveTrueOrderBySortOrderAscNameAsc());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return withCounts(categories.findAllByOrderBySortOrderAscNameAsc());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        Category category = categories.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("دسته‌بندی یافت نشد"));
        return mapper.toResponse(category, activeCounts().getOrDefault(category.getId(), 0L));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        String slug = resolveSlug(req, null);
        Category category = Category.builder()
                .name(req.name())
                .slug(slug)
                .description(req.description())
                .labelBg(blankToNull(req.labelBg()))
                .labelText(blankToNull(req.labelText()))
                .sortOrder(req.sortOrder() == null ? 0 : req.sortOrder())
                .iconUrl(req.iconUrl())
                .active(req.active())
                .build();
        return mapper.toResponse(categories.save(category), 0);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new NotFoundException("دسته‌بندی یافت نشد"));

        if (categories.existsByNameAndIdNot(req.name(), id)) {
            throw new ConflictException("NAME_TAKEN", "دسته‌بندی دیگری با این نام وجود دارد");
        }
        category.setName(req.name());
        category.setSlug(resolveSlug(req, id));
        category.setDescription(req.description());
        category.setLabelBg(blankToNull(req.labelBg()));
        category.setLabelText(blankToNull(req.labelText()));
        if (req.sortOrder() != null) {
            category.setSortOrder(req.sortOrder());
        }
        category.setIconUrl(req.iconUrl());
        category.setActive(req.active());

        Category saved = categories.save(category);
        return mapper.toResponse(saved, activeCounts().getOrDefault(saved.getId(), 0L));
    }

    /**
     * Deleting a category in use requires nominating a replacement.
     *
     * <p>Previously this was a bare {@code deleteById}, so deleting a category that any request
     * referenced blew up on the foreign key and surfaced as a 500 with raw SQL in the body.
     *
     * @param replacementId category to move dependent requests to; required when any exist
     */
    @Transactional
    public void delete(Long id, Long replacementId) {
        Category target = categories.findById(id)
                .orElseThrow(() -> new NotFoundException("دسته‌بندی یافت نشد"));

        long dependents = requests.countByCategoryIdAndDeletedAtIsNull(id);
        if (dependents > 0) {
            if (replacementId == null || replacementId.equals(id)) {
                throw new ConflictException("CATEGORY_IN_USE",
                        "این دسته‌بندی در %d درخواست استفاده شده است. برای حذف، دسته‌بندی جایگزین را انتخاب کنید."
                                .formatted(dependents));
            }
            if (!categories.existsById(replacementId)) {
                throw new NotFoundException("دسته‌بندی جایگزین یافت نشد");
            }
            // Grant the replacement to every centre that held the old one first, so the reassigned
            // requests remain consistent with their centre's allowed categories.
            centers.grantReplacementCategory(id, replacementId);
            requests.reassignCategory(id, replacementId);
        }

        centers.detachCategoryFromAllCenters(id);
        categories.delete(target);
    }

    private List<CategoryResponse> withCounts(List<Category> list) {
        Map<Long, Long> counts = activeCounts();
        return list.stream()
                .map(category -> mapper.toResponse(category, counts.getOrDefault(category.getId(), 0L)))
                .toList();
    }

    /** One grouped query for the whole list rather than a count per category. */
    private Map<Long, Long> activeCounts() {
        Map<Long, Long> counts = new HashMap<>();
        requests.countActiveByCategory(RequestStatus.PUBLISHED)
                .forEach(row -> counts.put(row.getCategoryId(), row.getTotal()));
        return counts;
    }

    /** Falls back to deriving the slug from the name when the admin leaves the field blank. */
    private String resolveSlug(CategoryRequest req, Long currentId) {
        String slug = blankToNull(req.slug());
        if (slug == null) {
            slug = SlugUtil.slugify(req.name(), 120);
        }
        if (slug.isBlank()) {
            throw new ValidationException("امکان ساخت نشانی یکتا از این نام وجود ندارد؛ آن را دستی وارد کنید");
        }
        boolean taken = currentId == null
                ? categories.existsBySlug(slug)
                : categories.existsBySlugAndIdNot(slug, currentId);
        if (taken) {
            throw new ConflictException("SLUG_TAKEN", "دسته‌بندی دیگری با این نشانی یکتا وجود دارد");
        }
        return slug;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
