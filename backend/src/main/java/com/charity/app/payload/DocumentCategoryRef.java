package com.charity.app.payload;

import com.charity.app.model.enums.DocumentScope;

/**
 * How a document category appears when embedded in a document. Enough for the browser to group a
 * flat list by category without a second request; the full record lives in
 * {@link DocumentCategoryResponse}.
 */
public record DocumentCategoryRef(Long id,
                                  String name,
                                  String slug,
                                  DocumentScope scope) {
}
