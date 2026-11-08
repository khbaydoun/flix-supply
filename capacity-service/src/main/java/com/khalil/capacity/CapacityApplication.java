package com.khalil.capacity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafkaStreams
public class CapacityApplication {
    public static void main(String[] args) {
        SpringApplication.run(CapacityApplication.class, args);
    }
}
