package org.iofstorm.tengu.tengutravels.model;

import java.util.List;

public class ShortVisits {

    private List<ShortVisit> visits;

    public ShortVisits() {
    }

    public ShortVisits(List<ShortVisit> visits) {
        this.visits = visits;
    }

    public List<ShortVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<ShortVisit> visits) {
        this.visits = visits;
    }
}
