package org.iofstorm.tengu.tengutravels;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public final class Utils {
    private Utils() {}

    public static Integer calcAge(long birthDate) {
        LocalDate bd = LocalDateTime.ofEpochSecond(birthDate, 0, ZoneOffset.UTC).toLocalDate();
        return Long.valueOf(ChronoUnit.YEARS.between(bd, LocalDate.now())).intValue();
    }
}
