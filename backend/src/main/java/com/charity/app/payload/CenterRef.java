package com.charity.app.payload;

/**
 * How a centre appears on a request card or detail sidebar.
 *
 * <p>The location lives here and nowhere else. A request used to carry its own city, copied out of
 * the centre's when the column was introduced, which gave the same fact two places to be wrong in.
 * {@code provinceName} is flattened alongside the city for the same reason {@link CityRef} does it:
 * so the sidebar can print «کرمان، کرمان» without a nested object or a second request.
 */
public record CenterRef(Long id,
                        String name,
                        String slug,
                        String logoUrl,
                        String contactPhone,
                        String responseHours,
                        String cityName,
                        String provinceName,
                        Long activeRequestCount) {
}
