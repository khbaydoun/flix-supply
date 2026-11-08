package com.khalil.capacity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.JacksonUtils;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

/**
 * supply.ride-events (keyed by rideId)
 *   → KTable of latest ride state
 *   → re-grouped by line code
 *   → aggregated into {@link LineCapacity} (store: {@value #STORE})
 *   → capacity.line-capacity (keyed by line code)
 *
 * Because the source is a table, a cancellation (or any update) retracts the ride's
 * previous contribution via the subtractor before adding the new one.
 */
@Component
public class CapacityTopology {

    public static final String RIDE_EVENTS = "supply.ride-events";
    public static final String LINE_CAPACITY = "capacity.line-capacity";
    public static final String STORE = "line-capacity-store";

    private static final ObjectMapper MAPPER = JacksonUtils.enhancedObjectMapper();

    static <T> Serde<T> json(Class<T> type) {
        JsonSerde<T> serde = new JsonSerde<>(type, MAPPER);
        serde.noTypeInfo().ignoreTypeHeaders();
        return serde;
    }

    @Autowired
    public void build(StreamsBuilder builder) {
        Serde<RideEvent> rideSerde = json(RideEvent.class);
        Serde<LineCapacity> capacitySerde = json(LineCapacity.class);

        KTable<String, RideEvent> rides = builder.table(RIDE_EVENTS, Consumed.with(Serdes.String(), rideSerde));

        KTable<String, LineCapacity> capacityByLine = rides
                .groupBy((rideId, ride) -> KeyValue.pair(ride.lineCode(), ride),
                        Grouped.with("rides-by-line", Serdes.String(), rideSerde))
                .aggregate(LineCapacity::empty,
                        (line, ride, agg) -> agg.add(line, ride),
                        (line, ride, agg) -> agg.subtract(line, ride),
                        Materialized.<String, LineCapacity, KeyValueStore<Bytes, byte[]>>as(STORE)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(capacitySerde));

        capacityByLine.toStream().to(LINE_CAPACITY, Produced.with(Serdes.String(), capacitySerde));
    }
}
