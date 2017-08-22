package org.iofstorm.tengu.tengutravels.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDate;
import java.time.ZoneId;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Location {
    public static final String ID = "id";
    public static final String PLACE = "place";
    public static final String COUNTRY = "country";
    public static final String CITY = "city";
    public static final String DISTANCE = "distance";

    public static final Integer COUNTRY_LENGTH = 50;
    public static final Integer CITY_LENGTH = 50;
    public static final Long VISITED_AT_MIN = LocalDate.of(2000, 1, 1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();
    public static final Long VISITED_AT_MAX = LocalDate.of(2015, 1, 1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();

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
