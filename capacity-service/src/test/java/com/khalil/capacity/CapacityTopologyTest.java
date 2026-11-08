package com.khalil.capacity;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class CapacityTopologyTest {

    private TopologyTestDriver driver;
    private TestInputTopic<String, RideEvent> rides;
    private TestOutputTopic<String, LineCapacity> capacity;
    private KeyValueStore<String, LineCapacity> store;

    @BeforeEach
    void setUp() {
        StreamsBuilder builder = new StreamsBuilder();
        new CapacityTopology().build(builder);

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "capacity-test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        props.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 0);

        driver = new TopologyTestDriver(builder.build(), props);
        rides = driver.createInputTopic(CapacityTopology.RIDE_EVENTS,
                Serdes.String().serializer(), CapacityTopology.json(RideEvent.class).serializer());
        capacity = driver.createOutputTopic(CapacityTopology.LINE_CAPACITY,
                Serdes.String().deserializer(), CapacityTopology.json(LineCapacity.class).deserializer());
        store = driver.getKeyValueStore(CapacityTopology.STORE);
    }

    @AfterEach
    void tearDown() {
        driver.close();
    }

    @Test
    void sumsScheduledSeatsPerLine() {
        rides.pipeInput("1", ride(1, "N120", 52, "SCHEDULED"));
        rides.pipeInput("2", ride(2, "N120", 49, "SCHEDULED"));
        rides.pipeInput("3", ride(3, "T15", 300, "SCHEDULED"));

        assertThat(store.get("N120")).isEqualTo(new LineCapacity("N120", 2, 101));
        assertThat(store.get("T15")).isEqualTo(new LineCapacity("T15", 1, 300));
    }

    @Test
    void cancellationRetractsSeats() {
        rides.pipeInput("1", ride(1, "N120", 52, "SCHEDULED"));
        rides.pipeInput("2", ride(2, "N120", 49, "SCHEDULED"));
        rides.pipeInput("2", ride(2, "N120", 49, "CANCELLED"));

        assertThat(store.get("N120")).isEqualTo(new LineCapacity("N120", 1, 52));
        assertThat(capacity.readValuesToList()).last().isEqualTo(new LineCapacity("N120", 1, 52));
    }

    @Test
    void replayingSameStateIsIdempotent() {
        rides.pipeInput("1", ride(1, "N120", 52, "SCHEDULED"));
        rides.pipeInput("1", ride(1, "N120", 52, "SCHEDULED"));

        assertThat(store.get("N120")).isEqualTo(new LineCapacity("N120", 1, 52));
    }

    private static RideEvent ride(long id, String line, int seats, String status) {
        Instant dep = Instant.parse("2026-12-01T10:00:00Z");
        return new RideEvent("RIDE_SNAPSHOT", id, line, 1L, "FLIXBUS", dep, dep.plusSeconds(3600),
                seats, status, Instant.now());
    }
}
