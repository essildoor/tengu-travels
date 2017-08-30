package org.iofstorm.tengu.tengutravels;

import org.iofstorm.tengu.tengutravels.loader.DataLoader;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Component
public class Utils {

    public static final String MALE = "m";
    public static final String FEMALE = "f";

    public int calcAge(long birthDate) {
        LocalDate bd = LocalDateTime.ofEpochSecond(birthDate, 0, ZoneOffset.UTC).toLocalDate();
        return (int)ChronoUnit.YEARS.between(bd, DataLoader.NOW_TS);
    }

    public boolean notMorF(String s) {
        return !MALE.equals(s) && !FEMALE.equals(s);
    }
}
