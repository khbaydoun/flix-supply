package com.khalil.supply.ride;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ride_stop")
@Getter
@Setter
@NoArgsConstructor
public class RideStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "station_name", nullable = false)
    private String stationName;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;
}
