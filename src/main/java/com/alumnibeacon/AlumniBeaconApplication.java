package com.alumnibeacon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlumniBeaconApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlumniBeaconApplication.class, args);
    }
}
