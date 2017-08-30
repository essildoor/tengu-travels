package org.iofstorm.tengu.tengutravels.model;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.math.BigDecimal;

public class Mark {

    private BigDecimal avg;

    public Mark() {
    }

    public Mark(BigDecimal avg) {
        this.avg = avg;
    }

    public BigDecimal getAvg() {
        return avg;
    }

    public void setAvg(BigDecimal avg) {
        this.avg = avg;
    }

    public static class MarkAdapter extends TypeAdapter<Mark> {

        @Override
        public void write(JsonWriter out, Mark value) throws IOException {
            out.beginObject();
            out.name("avg").value(value.getAvg());
            out.endObject();
        }

        @Override
        public Mark read(JsonReader in) throws IOException {
            return null; // not used
        }
    }
}
