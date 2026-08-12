package com.charity.app.payload;

/**
 * A city, flattened with its province name.
 *
 * <p>Public endpoints used to return the raw City entity, which forced City.province to stay EAGER
 * and serialised a nested province object on every row.
 */
public record CityRef(Long id,
                      String name,
                      Long provinceId,
                      String provinceName) {
}
