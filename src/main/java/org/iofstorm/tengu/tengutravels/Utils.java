package org.iofstorm.tengu.tengutravels;

import org.iofstorm.tengu.tengutravels.loader.DataLoader;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Component
public class Utils {

    public Integer calcAge(long birthDate) {
        LocalDate bd = LocalDateTime.ofEpochSecond(birthDate, 0, ZoneOffset.UTC).toLocalDate();
        return Long.valueOf(ChronoUnit.YEARS.between(bd, DataLoader.NOW_TS)).intValue();
    }

    public boolean notMorF(String s) {
        return !"m".equals(s) && !"f".equals(s);
    }
}
