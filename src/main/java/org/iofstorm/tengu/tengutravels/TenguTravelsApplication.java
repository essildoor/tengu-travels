package org.iofstorm.tengu.tengutravels;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class TenguTravelsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenguTravelsApplication.class, args);
    }
}
