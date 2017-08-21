package org.iofstorm.tengu.tengutravels;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iofstorm.tengu.tengutravels.loader.DataLoader;
import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.model.User;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    public void setCachedResponse(Object entity, ObjectMapper objectMapper) {
        try {
            String body = objectMapper.writeValueAsString(entity);
            ResponseEntity<String> res = ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(body.getBytes().length)
                    .body(body);
            if (entity instanceof User) ((User) entity).setCachedResponse(res);
            else if (entity instanceof Location) ((Location) entity).setCachedResponse(res);
        } catch (JsonProcessingException e) {
            System.out.println("fffuuuu");
        }
    }
}
