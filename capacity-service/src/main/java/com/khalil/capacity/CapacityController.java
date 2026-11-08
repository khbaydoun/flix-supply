package com.khalil.capacity;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/** Interactive queries against the local {@value CapacityTopology#STORE} state store. */
@RestController
@RequestMapping("/capacity")
public class CapacityController {

    private final StreamsBuilderFactoryBean streamsFactory;

    public CapacityController(StreamsBuilderFactoryBean streamsFactory) {
        this.streamsFactory = streamsFactory;
    }

    @GetMapping
    public List<LineCapacity> all() {
        List<LineCapacity> result = new ArrayList<>();
        try (var it = store().all()) {
            it.forEachRemaining(kv -> result.add(kv.value));
        }
        return result;
    }

    @GetMapping("/{lineCode}")
    public LineCapacity byLine(@PathVariable String lineCode) {
        LineCapacity value = store().get(lineCode);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No capacity known for line " + lineCode);
        }
        return value;
    }

    private ReadOnlyKeyValueStore<String, LineCapacity> store() {
        KafkaStreams streams = streamsFactory.getKafkaStreams();
        if (streams == null || streams.state() != KafkaStreams.State.RUNNING) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Kafka Streams not running (state: " + (streams == null ? "NOT_CREATED" : streams.state()) + ")");
        }
        return streams.store(StoreQueryParameters.fromNameAndType(
                CapacityTopology.STORE, QueryableStoreTypes.keyValueStore()));
    }
}
