package org.iofstorm.tengu.tengutravels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.model.Mark;
import org.iofstorm.tengu.tengutravels.model.ShortVisit;
import org.iofstorm.tengu.tengutravels.model.User;
import org.iofstorm.tengu.tengutravels.model.Visit;
import org.iofstorm.tengu.tengutravels.service.LocationService;
import org.iofstorm.tengu.tengutravels.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

@SpringBootApplication
public class TenguTravelsApplication {

    public static long startTs;

    @Bean
    @Primary
    public Gson gson() {
        return new GsonBuilder()
                .disableHtmlEscaping()
                .disableInnerClassSerialization()
                .registerTypeAdapter(Location.class, new Location.LocationAdapter())
                .registerTypeAdapter(User.class, new User.UserAdapter())
                .registerTypeAdapter(Visit.class, new Visit.VisitAdapter())
                .registerTypeAdapter(ShortVisit.class, new ShortVisit.ShortVisitAdapter())
                .registerTypeAdapter(Mark.class, new Mark.MarkAdapter())
                .create();
    }

    @Bean
    public GsonHttpMessageConverter gsonHttpMessageConverter(Gson gson) {
        GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
        converter.setGson(gson);
        return converter;
    }

    public static void main(String[] args) {
        startTs = System.currentTimeMillis();
        SpringApplication.run(TenguTravelsApplication.class, args);
    }
}
