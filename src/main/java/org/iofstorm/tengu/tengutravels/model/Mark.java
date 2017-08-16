package org.iofstorm.tengu.tengutravels.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class Mark extends Entity {

    @JsonProperty
    private Double mark;

    public Double getMark() {
        return mark;
    }

    public void setMark(Double mark) {
        BigDecimal bd = BigDecimal.valueOf(mark).setScale(5, BigDecimal.ROUND_HALF_UP);
        this.mark = bd.doubleValue();
    }
}
