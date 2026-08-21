package com.charity.app.payload;

import java.time.OffsetDateTime;

/**
 * One document belonging to a centre. Same shape as {@link RequestDocumentResponse}, kept separate
 * so the two can diverge without a shared type forcing a change on both public pages at once.
 */
public record CenterDocumentResponse(Long id,
                                     String url,
                                     String title,
                                     String originalFilename,
                                     String contentType,
                                     Long sizeBytes,
                                     DocumentCategoryRef category,
                                     OffsetDateTime uploadedAt) {
}
