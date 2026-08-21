package com.charity.app.mapper;

import com.charity.app.common.AppUrls;
import com.charity.app.model.CenterDocument;
import com.charity.app.model.DocumentCategory;
import com.charity.app.model.RequestDocument;
import com.charity.app.payload.CenterDocumentResponse;
import com.charity.app.payload.DocumentCategoryRef;
import com.charity.app.payload.DocumentCategoryResponse;
import com.charity.app.payload.RequestDocumentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * The one place a stored filename becomes a URL.
 *
 * <p>Depends on {@link AppUrls} and nothing else, so it can be injected into {@code RequestMapper}
 * and {@code CenterMapper} without closing a bean cycle.
 */
@Component
@RequiredArgsConstructor
public class DocumentMapper {

    private final AppUrls urls;

    /**
     * Ordering is applied here rather than by a JPA {@code @OrderBy} on the collection, because it
     * reaches through the category association: category sort order first, then the document's own
     * position, then id as the tie-break that keeps the list stable.
     */
    private static final Comparator<RequestDocument> REQUEST_ORDER =
            Comparator.<RequestDocument>comparingInt(d -> categorySort(d.getCategory()))
                    .thenComparing(RequestDocument::getSortOrder)
                    .thenComparing(RequestDocument::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<CenterDocument> CENTER_ORDER =
            Comparator.<CenterDocument>comparingInt(d -> categorySort(d.getCategory()))
                    .thenComparing(CenterDocument::getSortOrder)
                    .thenComparing(CenterDocument::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    public List<RequestDocumentResponse> toRequestDocuments(List<RequestDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream().sorted(REQUEST_ORDER).map(this::toResponse).toList();
    }

    public List<CenterDocumentResponse> toCenterDocuments(List<CenterDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream().sorted(CENTER_ORDER).map(this::toResponse).toList();
    }

    public RequestDocumentResponse toResponse(RequestDocument document) {
        return new RequestDocumentResponse(
                document.getId(),
                urls.fileUrl(document.getStoredFilename()),
                document.getTitle(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                toRef(document.getCategory()),
                urls.iso(document.getUploadedAt()));
    }

    public CenterDocumentResponse toResponse(CenterDocument document) {
        return new CenterDocumentResponse(
                document.getId(),
                urls.fileUrl(document.getStoredFilename()),
                document.getTitle(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                toRef(document.getCategory()),
                urls.iso(document.getUploadedAt()));
    }

    public DocumentCategoryRef toRef(DocumentCategory category) {
        if (category == null) {
            return null;
        }
        return new DocumentCategoryRef(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getScope());
    }

    public DocumentCategoryResponse toResponse(DocumentCategory category) {
        return new DocumentCategoryResponse(
                category.getId(),
                category.getScope(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getSortOrder(),
                category.isActive(),
                urls.iso(category.getUpdatedAt()));
    }

    /** A document whose category somehow went missing sorts last rather than throwing. */
    private static int categorySort(DocumentCategory category) {
        return category == null ? Integer.MAX_VALUE : category.getSortOrder();
    }
}
