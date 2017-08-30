package org.iofstorm.tengu.tengutravels.model;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;

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
    Integer id;

    // unbounded string
    String place;

    // unicode string 0-50
    String country;

    // unicode string 0-50
    private String city;

    // 32 bit int
    int distance;

    public Location() {
    }

    public Location(Integer id, String place, String country, String city, Integer distance) {
        this.id = id;
        this.place = place;
        this.country = country;
        this.city = city;
        this.distance = distance;
    }

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

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public static class LocationAdapter extends TypeAdapter<Location> {

        @Override
        public void write(JsonWriter out, Location value) throws IOException {
            out.beginObject();
            out.name(ID).value(value.getId());
            out.name(PLACE).value(value.getPlace());
            out.name(COUNTRY).value(value.getCountry());
            out.name(CITY).value(value.getCity());
            out.name(DISTANCE).value(value.getDistance());
            out.endObject();
        }

        @Override
        public Location read(JsonReader in) throws IOException {
            Location location = new Location();
            in.beginObject();
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case ID:
                        location.setId(in.nextInt());
                        break;
                    case PLACE:
                        location.setPlace(in.nextString());
                        break;
                    case COUNTRY:
                        location.setCountry(in.nextString());
                        break;
                    case CITY:
                        location.setCity(in.nextString());
                        break;
                    case DISTANCE:
                        location.setDistance(in.nextInt());
                }
            }
            in.endObject();
            return location;
        }
    }
}
