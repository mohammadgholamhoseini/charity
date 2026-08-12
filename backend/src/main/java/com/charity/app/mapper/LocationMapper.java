package com.charity.app.mapper;

import com.charity.app.model.City;
import com.charity.app.payload.CityRef;
import org.springframework.stereotype.Component;

/**
 * Flattens a city with its province name.
 *
 * <p>Public endpoints used to return the raw City entity, which is why {@code City.province} had to
 * stay EAGER and every city in a dropdown dragged a nested province object with it.
 */
@Component
public class LocationMapper {

    public CityRef toRef(City city) {
        if (city == null) {
            return null;
        }
        return new CityRef(
                city.getId(),
                city.getName(),
                city.getProvince() == null ? null : city.getProvince().getId(),
                city.getProvince() == null ? null : city.getProvince().getName());
    }
}
