package org.iofstorm.tengu.tengutravels.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Visit {
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

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("locationId", locationId)
                .append("userId", userId)
                .append("visitedAt", visitedAt)
                .append("mark", mark)
                .build();
    }
}
