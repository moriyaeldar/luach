package io.github.moriyaeldar.luach.household;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HouseholdServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HouseholdServiceApplication.class, args);
    }
}
