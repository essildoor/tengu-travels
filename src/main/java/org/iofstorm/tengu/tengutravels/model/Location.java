package org.iofstorm.tengu.tengutravels.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Location {
    // 32 bit int unique
    private Integer id;

    // unbounded string
    private String place;

    // unicode string 0-50
    private String country;

    // unicode string 0-50
    private String city;

    // 32 bit int
    private Integer distance;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("place", place)
                .append("country", country)
                .append("city", city)
                .append("distance", distance)
                .build();
    }
}