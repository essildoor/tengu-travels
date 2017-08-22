package org.iofstorm.tengu.tengutravels;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TenguTravelsApplication {

    public static long startTs;

    public static void main(String[] args) {
        startTs = System.currentTimeMillis();
        SpringApplication.run(TenguTravelsApplication.class, args);
    }
}
