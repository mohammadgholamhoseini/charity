package com.charity.app.mapper;

import com.charity.app.common.AppUrls;
import com.charity.app.model.Category;
import com.charity.app.model.Center;
import com.charity.app.payload.CategoryRef;
import com.charity.app.payload.CenterCard;
import com.charity.app.payload.CenterPublicProfile;
import com.charity.app.payload.CenterRef;
import com.charity.app.payload.CenterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * The three -- and only three -- shapes a centre is exposed in.
 *
 * <p>Which fields are admin-only is now an explicit decision rather than an accident of which
 * endpoint you called: {@code username} and {@code email} appear in {@link CenterResponse} alone.
 * Banking details stay public on purpose, because there is no online payment and a visitor who
 * wants to help pays the centre directly.
 */
@Component
@RequiredArgsConstructor
public class CenterMapper {

    private final CategoryMapper categoryMapper;
    private final LocationMapper locationMapper;
    private final AppUrls urls;

    /** Embedded in a request card or detail sidebar. */
    public CenterRef toRef(Center center, long activeRequestCount) {
        if (center == null) {
            return null;
        }
        return new CenterRef(
                center.getId(),
                center.getName(),
                center.getSlug(),
                urls.fileUrl(center.getLogoUrl()),
                center.getContactPhone(),
                center.getResponseHours(),
                center.getCity() == null ? null : center.getCity().getName(),
                center.getCity() == null || center.getCity().getProvince() == null
                        ? null
                        : center.getCity().getProvince().getName(),
                activeRequestCount);
    }

    /** Public centres listing. */
    public CenterCard toCard(Center center, long activeRequestCount) {
        return new CenterCard(
                center.getId(),
                center.getName(),
                center.getSlug(),
                urls.fileUrl(center.getLogoUrl()),
                center.getDescription(),
                locationMapper.toRef(center.getCity()),
                sortedCategories(center),
                activeRequestCount);
    }

    /** Public centre page. */
    public CenterPublicProfile toPublicProfile(Center center, long activeRequestCount) {
        return new CenterPublicProfile(
                center.getId(),
                center.getName(),
                center.getSlug(),
                urls.centerUrl(center.getSlug()),
                center.getFullName(),
                center.getDescription(),
                center.getContactPhone(),
                center.getResponseHours(),
                center.getAddress(),
                center.getCardNumber(),
                center.getSheba(),
                urls.fileUrl(center.getLogoUrl()),
                locationMapper.toRef(center.getCity()),
                sortedCategories(center),
                activeRequestCount,
                urls.iso(center.getUpdatedAt()));
    }

    /** The centre's own profile screen, and admin screens. */
    public CenterResponse toResponse(Center center, long activeRequestCount) {
        return new CenterResponse(
                center.getId(),
                center.getName(),
                center.getSlug(),
                center.getFullName(),
                center.getDescription(),
                center.getContactPhone(),
                center.getResponseHours(),
                center.getUser() == null ? null : center.getUser().getUsername(),
                center.getUser() == null ? null : center.getUser().getEmail(),
                center.getAddress(),
                center.getCardNumber(),
                center.getSheba(),
                urls.fileUrl(center.getLogoUrl()),
                center.getStatus(),
                center.getStatus() == null ? null : center.getStatus().label(),
                locationMapper.toRef(center.getCity()),
                sortedCategories(center),
                activeRequestCount,
                urls.iso(center.getCreatedAt()),
                urls.iso(center.getUpdatedAt()));
    }

    /**
     * Categories come out of a HashSet, so they must be sorted before being serialised. The old code
     * exposed an arbitrary element of that set as the centre's "primary category", which meant the
     * value could differ between two calls in the same deployment.
     */
    private List<CategoryRef> sortedCategories(Center center) {
        if (center.getCategories() == null) {
            return List.of();
        }
        return center.getCategories().stream()
                .sorted(Comparator.comparingInt(Category::getSortOrder).thenComparing(Category::getName))
                .map(categoryMapper::toRef)
                .toList();
    }
}
