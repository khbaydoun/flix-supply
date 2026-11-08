package com.khalil.supply;

import com.khalil.supply.events.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end against real Postgres + Kafka. Skipped automatically when Docker isn't available. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class SupplyApplicationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Autowired
    TestRestTemplate http;

    @Test
    void listsSeededBrands() {
        var res = http.getForEntity("/brands", List.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).hasSize(3);
    }

    @Test
    void unknownBrandIs404() {
        assertThat(http.getForEntity("/brands/NOPE", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rideInheritsLineBrandUnlessOverridden() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rides = http.getForObject("/rides?lineCode=N120", List.class);
        assertThat(rides).extracting(r -> r.get("brandCode")).containsExactly("FLIXBUS", "KAMIL_KOC");
    }

    @Test
    void createAndCancelRidePublishesEvents() {
        Instant dep = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        String body = """
                {"lineCode":"N33","partnerId":1,"departureAt":"%s","arrivalAt":"%s","capacity":40,
                 "stops":[{"stationName":"Köln Hbf","scheduledAt":"%s"}]}
                """.formatted(dep, dep.plus(7, ChronoUnit.HOURS), dep);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var created = http.postForEntity("/rides", new HttpEntity<>(body, headers), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Number id = (Number) ((Map<String, Object>) created.getBody().get("ride")).get("id");

        var cancelled = http.postForEntity("/rides/" + id + "/cancel", null, Map.class);
        assertThat(cancelled.getBody()).containsEntry("status", "CANCELLED");
        assertThat(http.postForEntity("/rides/" + id + "/cancel", null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        try (var consumer = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(KafkaTopics.RIDE_EVENTS));
            List<String> values = new java.util.ArrayList<>();
            long deadline = System.currentTimeMillis() + 15_000;
            while (values.size() < 2 && System.currentTimeMillis() < deadline) {
                consumer.poll(Duration.ofMillis(500)).forEach(r -> {
                    if (r.key().equals(String.valueOf(id))) values.add(r.value());
                });
            }
            assertThat(values).hasSize(2);
            assertThat(values.get(0)).contains("RIDE_SCHEDULED");
            assertThat(values.get(1)).contains("RIDE_CANCELLED");
        }
    }

    @Test
    void invalidRequestIs400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var res = http.postForEntity("/rides", new HttpEntity<>("{\"capacity\":0}", headers), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
