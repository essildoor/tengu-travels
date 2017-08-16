package org.iofstorm.tengu.tengutravels;

import java.time.LocalDate;
import java.time.ZoneId;

public final class Constants {
    private Constants() {}

    // response codes
    public static final int OK = 200;
    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND = 404;

    // user constants
    public static final Integer EMAIL_LENGTH = 100;
    public static final Integer NAME_LENGTH = 50;
    public static final Long BIRTH_DATE_MIN = LocalDate.of(1930, 1, 1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();
    public static final Long BIRTH_DATE_MAX = LocalDate.of(1999, 1, 1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();
    public static final Integer MAX_AGE = Utils.calcAge(BIRTH_DATE_MIN);
    public static final Integer MIN_AGE = Utils.calcAge(BIRTH_DATE_MAX);

    // location constants
    public static final Integer COUNTRY_LENGTH = 50;
    public static final Integer CITY_LENGTH = 50;
    public static final Long VISITED_AT_MIN = LocalDate.of(2000, 1, 1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();
    public static final Long VISITED_AT_MAX = LocalDate.of(2015, 1, 1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();
}
