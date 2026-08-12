package com.charity.app.mapper;

import com.charity.app.common.AppUrls;
import com.charity.app.model.Request;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.payload.RequestDetailResponse;
import com.charity.app.payload.RequestSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RequestMapper {

    /** Amounts are quoted in toman on the site; naming the unit avoids guesswork downstream. */
    private static final String CURRENCY = "IRT";

    private static final int SUMMARY_LENGTH = 160;

    private final CategoryMapper categoryMapper;
    private final CenterMapper centerMapper;
    private final LocationMapper locationMapper;
    private final AppUrls urls;

    public RequestSummary toSummary(Request request) {
        return new RequestSummary(
                request.getId(),
                request.getCode(),
                request.getSlug(),
                request.getTitle(),
                summarize(request.getDescription()),
                request.getAmountNeeded(),
                request.getDeadline(),
                request.getStatus(),
                request.getStatus() == null ? null : request.getStatus().label(),
                request.getUrgency(),
                request.getUrgency() == null ? null : request.getUrgency().label(),
                categoryMapper.toRef(request.getCategory()),
                centerMapper.toRef(request.getCenter(), 0),
                locationMapper.toRef(request.getCity()),
                urls.iso(request.getCreatedAt()),
                urls.iso(request.getPublishedAt()),
                urls.iso(request.getUpdatedAt()));
    }

    /**
     * @param includePrivateNotes true for the owning centre and for admins. The status note explains
     *                            why something was rejected and is not for public consumption.
     */
    public RequestDetailResponse toDetail(Request request,
                                          long centerActiveRequests,
                                          boolean includePrivateNotes) {
        return new RequestDetailResponse(
                request.getId(),
                request.getCode(),
                request.getSlug(),
                urls.requestUrl(request.getSlug()),
                request.getTitle(),
                summarize(request.getDescription()),
                request.getDescription(),
                request.getAmountNeeded(),
                CURRENCY,
                request.getDeadline(),
                request.getStatus(),
                request.getStatus() == null ? null : request.getStatus().label(),
                request.getStatus() == RequestStatus.PUBLISHED,
                includePrivateNotes ? request.getStatusNote() : null,
                request.getUrgency(),
                request.getUrgency() == null ? null : request.getUrgency().label(),
                categoryMapper.toRef(request.getCategory()),
                centerMapper.toRef(request.getCenter(), centerActiveRequests),
                locationMapper.toRef(request.getCity()),
                urls.fileUrl(request.getImageUrl()),
                request.getContactInfo(),
                documentUrls(request),
                request.getDetails() == null ? Map.of() : request.getDetails(),
                metaTitle(request),
                metaDescription(request),
                urls.iso(request.getCreatedAt()),
                urls.iso(request.getPublishedAt()),
                urls.iso(request.getUpdatedAt()));
    }

    private List<String> documentUrls(Request request) {
        if (request.getDocuments() == null) {
            return List.of();
        }
        return request.getDocuments().stream().map(urls::fileUrl).toList();
    }

    /** Admin override wins; otherwise build something reasonable from the title and centre. */
    private String metaTitle(Request request) {
        if (request.getMetaTitle() != null && !request.getMetaTitle().isBlank()) {
            return request.getMetaTitle();
        }
        String centerName = request.getCenter() == null ? null : request.getCenter().getName();
        String base = centerName == null ? request.getTitle() : request.getTitle() + " — " + centerName;
        return truncate(base, 70);
    }

    private String metaDescription(Request request) {
        if (request.getMetaDescription() != null && !request.getMetaDescription().isBlank()) {
            return request.getMetaDescription();
        }
        return summarize(request.getDescription());
    }

    /** Truncates on a word boundary so a Persian description never ends mid-word. */
    private String summarize(String description) {
        return truncate(description, SUMMARY_LENGTH);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String collapsed = value.replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= max) {
            return collapsed;
        }
        String cut = collapsed.substring(0, max);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > max / 2) {
            cut = cut.substring(0, lastSpace);
        }
        return cut + "…";
    }
}
