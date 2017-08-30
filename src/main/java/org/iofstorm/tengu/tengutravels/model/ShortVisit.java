package org.iofstorm.tengu.tengutravels.model;


import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

import static org.iofstorm.tengu.tengutravels.model.Location.PLACE;
import static org.iofstorm.tengu.tengutravels.model.Visit.MARK;
import static org.iofstorm.tengu.tengutravels.model.Visit.VISITED_AT;

public class ShortVisit {

    private final int mark;
    private final long visitedAt;
    private final String place;

    public ShortVisit(int mark, long visitedAt, String place) {
        this.mark = mark;
        this.visitedAt = visitedAt;
        this.place = place;
    }

    public int getMark() {
        return mark;
    }

    public long getVisitedAt() {
        return visitedAt;
    }

    public String getPlace() {
        return place;
    }

    public static class ShortVisitAdapter extends TypeAdapter<ShortVisit> {

        @Override
        public void write(JsonWriter out, ShortVisit value) throws IOException {
            out.beginObject();
            out.name(MARK).value(value.getMark());
            out.name(VISITED_AT).value(value.getVisitedAt());
            out.name(PLACE).value(value.getPlace());
            out.endObject();
        }

        @Override
        public ShortVisit read(JsonReader in) throws IOException {
            return null; // not used
        }
    }
}
