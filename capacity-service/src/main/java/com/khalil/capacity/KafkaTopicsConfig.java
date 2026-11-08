package com.khalil.capacity;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Streams refuses to start if its source topic is missing, so this service
 * declares it too (KafkaAdmin creates topics before the streams app starts).
 * Settings must match those declared by supply-service.
 */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic rideEventsTopic() {
        return TopicBuilder.name(CapacityTopology.RIDE_EVENTS).partitions(3).replicas(1)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
                .build();
    }

    @Bean
    NewTopic lineCapacityTopic() {
        return TopicBuilder.name(CapacityTopology.LINE_CAPACITY).partitions(3).replicas(1)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
                .build();
    }
}
