package org.iofstorm.tengu.tengutravels.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Visits extends Entity {

    @JsonProperty
    private List<Visit> visits;

    public List<Visit> getVisits() {
        return visits;
    }

    public void setVisits(List<Visit> visits) {
        this.visits = visits;
    }
}
