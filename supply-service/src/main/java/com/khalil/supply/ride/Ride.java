package com.khalil.supply.ride;

import com.khalil.supply.brand.Brand;
import com.khalil.supply.line.Line;
import com.khalil.supply.partner.MobilityPartner;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ride")
@Getter
@Setter
@NoArgsConstructor
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false)
    private Line line;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private MobilityPartner partner;

    /** New: brand at ride level. {@code null} means "inherit from the line". */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "departure_at", nullable = false)
    private Instant departureAt;

    @Column(name = "arrival_at", nullable = false)
    private Instant arrivalAt;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status = RideStatus.SCHEDULED;

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNo ASC")
    private List<RideStop> stops = new ArrayList<>();

    /** Ride-level brand wins; otherwise fall back to the legacy line-level brand. */
    public Brand effectiveBrand() {
        return brand != null ? brand : line.getBrand();
    }

    public void addStop(int sequenceNo, String stationName, Instant scheduledAt) {
        RideStop stop = new RideStop();
        stop.setRide(this);
        stop.setSequenceNo(sequenceNo);
        stop.setStationName(stationName);
        stop.setScheduledAt(scheduledAt);
        stops.add(stop);
    }

    public void cancel() {
        if (status == RideStatus.CANCELLED) {
            throw new IllegalStateException("Ride " + id + " is already cancelled");
        }
        status = RideStatus.CANCELLED;
    }
}
