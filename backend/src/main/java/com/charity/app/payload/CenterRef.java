package com.charity.app.payload;

/** How a centre appears on a request card or detail sidebar. */
public record CenterRef(Long id,
                        String name,
                        String slug,
                        String logoUrl,
                        String contactPhone,
                        String responseHours,
                        String cityName,
                        Long activeRequestCount) {
}
