package org.iofstorm.tengu.tengutravels.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ShortVisits {

    @JsonProperty("visits")
    private List<ShortVisit> visits;

    public List<ShortVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<ShortVisit> visits) {
        this.visits = visits;
    }
}
