package org.iofstorm.tengu.tengutravels.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class Mark {

    @JsonProperty("avg")
    private BigDecimal avg;

    public BigDecimal getAvg() {
        return avg;
    }

    public void setAvg(BigDecimal avg) {
        this.avg = avg;
    }
}
