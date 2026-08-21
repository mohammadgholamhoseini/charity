package com.charity.app.payload;

import java.time.OffsetDateTime;

/**
 * One document attached to a request, as served publicly.
 *
 * <p>{@code url} is absolute and built by {@code AppUrls.fileUrl}, exactly like {@code imageUrl}
 * and {@code logoUrl}. The stored UUID filename is never exposed on its own -- there is nothing
 * useful a client could do with it, and it invites path-building.
 *
 * <p>Nulls are serialised rather than omitted: a title-less document is a normal case and the
 * client renders {@code originalFilename} in its place, which is easier to write against a field
 * that is reliably present.
 */
public record RequestDocumentResponse(Long id,
                                      String url,
                                      String title,
                                      String originalFilename,
                                      String contentType,
                                      Long sizeBytes,
                                      DocumentCategoryRef category,
                                      OffsetDateTime uploadedAt) {
}
