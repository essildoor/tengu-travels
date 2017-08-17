package org.iofstorm.tengu.tengutravels.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public class ShortVisit {

    private Integer mark;

    @JsonProperty("visited_at")
    private Long visitedAt;

    private String place;

    public ShortVisit() {
    }

    public ShortVisit(Integer mark, Long visitedAt, String place) {
        this.mark = mark;
        this.visitedAt = visitedAt;
        this.place = place;
    }

    public static ShortVisit fromVisit(Visit v) {
        return new ShortVisit(v.getMark(), v.getVisitedAt(), v.getLocationPlace());
    }

    public Integer getMark() {
        return mark;
    }

    public void setMark(Integer mark) {
        this.mark = mark;
    }

    public Long getVisitedAt() {
        return visitedAt;
    }

    public void setVisitedAt(Long visitedAt) {
        this.visitedAt = visitedAt;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }
}
