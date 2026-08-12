package com.charity.app.payload;

/** How a category appears when embedded in another response. Carries the chip colours with it. */
public record CategoryRef(Long id,
                          String name,
                          String slug,
                          String labelBg,
                          String labelText) {
}
