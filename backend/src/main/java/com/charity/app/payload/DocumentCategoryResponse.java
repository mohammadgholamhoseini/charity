package com.charity.app.payload;

import com.charity.app.model.enums.DocumentScope;

import java.time.OffsetDateTime;

/**
 * A document category as the admin table and the upload pickers see it.
 *
 * <p>No usage count: unlike the need taxonomy there is nothing public to show it next to, and the
 * only place the number matters is the 409 an admin gets when deleting a category still in use,
 * which carries it in its message.
 */
public record DocumentCategoryResponse(Long id,
                                       DocumentScope scope,
                                       String name,
                                       String slug,
                                       String description,
                                       int sortOrder,
                                       boolean active,
                                       OffsetDateTime updatedAt) {
}
