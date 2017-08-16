package org.iofstorm.tengu.tengutravels.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Locations {

    @JsonProperty
    private List<Location> locations;

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }
}
