package com.listingsearch;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Entry point. Starts the embedded web server on port 8080. */
@SpringBootApplication
public class ListingSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ListingSearchApplication.class, args);
    }

    /** The source of "today" for recency scoring. Tests swap in a fixed clock. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
