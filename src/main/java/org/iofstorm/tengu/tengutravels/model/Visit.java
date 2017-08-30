package org.iofstorm.tengu.tengutravels.model;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.iofstorm.tengu.tengutravels.service.LocationService;
import org.iofstorm.tengu.tengutravels.service.UserService;

import java.io.IOException;

public class Visit {
    public static final String LOCATION_ID = "location";
    public static final String USER_ID = "user";
    public static final String VISITED_AT = "visited_at";
    public static final String MARK = "mark";
    public static final String ID = "id";

    // 32 bit int unique
    private Integer id;

    // timestamp, 01.01.2000 - 01.01.2015
    private long visitedAt = Long.MIN_VALUE;

    // int 0 - 5
    private int mark = Integer.MIN_VALUE;

    public Location location;

    public User user;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getLocationId() {
        return location == null ? null : location.id;
    }

    public Integer getUserId() {
        return user == null ? null : user.id;
    }

    public long getVisitedAt() {
        return visitedAt;
    }

    public void setVisitedAt(long visitedAt) {
        this.visitedAt = visitedAt;
    }

    public int getMark() {
        return mark;
    }

    public void setMark(int mark) {
        this.mark = mark;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLocationCountry() {
        return location.country;
    }

    public int getLocationDistance() {
        return location.distance;
    }

    public String getLocationPlace() {
        return location.place;
    }

    public int getUserAge() {
        return user.age;
    }

    public Gender getUserGender() {
        return user.gender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Visit visit = (Visit) o;

        return visitedAt == visit.visitedAt;
    }

    @Override
    public int hashCode() {
        return (int) (visitedAt ^ (visitedAt >>> 32));
    }

    public static class VisitAdapter extends TypeAdapter<Visit> {

        @Override
        public void write(JsonWriter out, Visit value) throws IOException {
            out.beginObject();
            out.name(ID).value(value.getId());
            out.name(USER_ID).value(value.getUserId());
            out.name(LOCATION_ID).value(value.getLocationId());
            out.name(VISITED_AT).value(value.getVisitedAt());
            out.name(MARK).value(value.getMark());
            out.endObject();
        }

        @Override
        public Visit read(JsonReader in) throws IOException {
            Visit visit = new Visit();
            in.beginObject();
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case ID:
                        visit.setId(in.nextInt());
                        break;
                    case USER_ID:
                        visit.setUser(UserService.users.get(in.nextInt()));
                        break;
                    case LOCATION_ID:
                        visit.setLocation(LocationService.locations.get(in.nextInt()));
                        break;
                    case VISITED_AT:
                        visit.setVisitedAt(in.nextLong());
                        break;
                    case MARK:
                        visit.setMark(in.nextInt());
                }
            }
            in.endObject();
            return visit;
        }
    }
}
