package com.charity.app.mapper;

import com.charity.app.common.AppUrls;
import com.charity.app.model.Category;
import com.charity.app.payload.CategoryRef;
import com.charity.app.payload.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryMapper {

    /** Fallback chip colours for categories created before the swatch picker existed. */
    private static final String DEFAULT_LABEL_BG = "#EFEAE3";
    private static final String DEFAULT_LABEL_TEXT = "#8A7F72";

    private final AppUrls urls;

    public CategoryRef toRef(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryRef(
                category.getId(),
                category.getName(),
                category.getSlug(),
                labelBg(category),
                labelText(category));
    }

    public CategoryResponse toResponse(Category category, long activeRequestCount) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                labelBg(category),
                labelText(category),
                category.getSortOrder(),
                category.getIconUrl(),
                category.isActive(),
                activeRequestCount,
                urls.iso(category.getUpdatedAt()));
    }

    private String labelBg(Category category) {
        return blankToNull(category.getLabelBg()) == null ? DEFAULT_LABEL_BG : category.getLabelBg();
    }

    private String labelText(Category category) {
        return blankToNull(category.getLabelText()) == null ? DEFAULT_LABEL_TEXT : category.getLabelText();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
