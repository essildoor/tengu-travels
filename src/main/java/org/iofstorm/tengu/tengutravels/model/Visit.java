package org.iofstorm.tengu.tengutravels.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Visit {
    public static final String LOCATION_ID = "location";
    public static final String USER_ID = "user";
    public static final String VISITED_AT = "visited_at";
    public static final String MARK = "mark";
    public static final String ID = "id";

    // 32 bit int unique
    private Integer id;

    // 32 bit int
    @JsonProperty("location")
    private Integer locationId;

    // 32 bit int
    @JsonProperty("user")
    private Integer userId;

    // timestamp, 01.01.2000 - 01.01.2015
    @JsonProperty("visited_at")
    private Long visitedAt;

    // int 0 - 5
    private Integer mark;

    @JsonIgnore
    private String locationCountry;

    @JsonIgnore
    private Integer locationDistance;

    @JsonIgnore
    private String locationPlace;

    @JsonIgnore
    private Integer userAge;

    @JsonIgnore
    private String userGender;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getLocationId() {
        return locationId;
    }

    public void setLocationId(Integer locationId) {
        this.locationId = locationId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Long getVisitedAt() {
        return visitedAt;
    }

    public void setVisitedAt(Long visitedAt) {
        this.visitedAt = visitedAt;
    }

    public Integer getMark() {
        return mark;
    }

    public void setMark(Integer mark) {
        this.mark = mark;
    }

    public String getLocationCountry() {
        return locationCountry;
    }

    public void setLocationCountry(String locationCountry) {
        this.locationCountry = locationCountry;
    }

    public Integer getLocationDistance() {
        return locationDistance;
    }

    public void setLocationDistance(Integer locationDistance) {
        this.locationDistance = locationDistance;
    }

    public String getLocationPlace() {
        return locationPlace;
    }

    public void setLocationPlace(String locationPlace) {
        this.locationPlace = locationPlace;
    }

    public Integer getUserAge() {
        return userAge;
    }

    public void setUserAge(Integer userAge) {
        this.userAge = userAge;
    }

    public String getUserGender() {
        return userGender;
    }

    public void setUserGender(String userGender) {
        this.userGender = userGender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Visit visit = (Visit) o;

        return id.equals(visit.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
