package org.iofstorm.tengu.tengutravels.model;

import org.iofstorm.tengu.tengutravels.Utils;

public enum Gender {
    MALE(Utils.MALE),
    FEMALE(Utils.FEMALE),
    UNKNOWN(null);

    private String val;

    Gender(String val) {
        this.val = val;
    }

    public String getVal() {
        return val;
    }

    public static Gender fromString(String gender) {
        if (gender == null) return UNKNOWN;
        switch (gender) {
            case "m":
                return MALE;
            case "f":
                return FEMALE;
            default:
                return UNKNOWN;
        }
    }
}
