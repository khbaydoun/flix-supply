package com.khalil.supply.ride;

import com.khalil.supply.brand.BrandService;
import com.khalil.supply.events.RideEvent;
import com.khalil.supply.line.LineNotFoundException;
import com.khalil.supply.line.LineRepository;
import com.khalil.supply.partner.MobilityPartner;
import com.khalil.supply.partner.MobilityPartnerRepository;
import com.khalil.supply.partner.PartnerNotFoundException;
import com.khalil.supply.ride.dto.CreateRideRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RideService {

    private final RideRepository rides;
    private final LineRepository lines;
    private final MobilityPartnerRepository partners;
    private final BrandService brands;
    private final ApplicationEventPublisher events;

    public RideService(RideRepository rides, LineRepository lines, MobilityPartnerRepository partners,
                       BrandService brands, ApplicationEventPublisher events) {
        this.rides = rides;
        this.lines = lines;
        this.partners = partners;
        this.brands = brands;
        this.events = events;
    }

    public List<Ride> search(String lineCode, String brandCode) {
        return rides.search(lineCode, brandCode);
    }

    public Ride findDetailed(Long id) {
        return rides.findDetailedById(id).orElseThrow(() -> new RideNotFoundException(id));
    }

    @Transactional
    public Ride create(CreateRideRequest req) {
        MobilityPartner partner = partners.findById(req.partnerId())
                .orElseThrow(() -> new PartnerNotFoundException(req.partnerId()));
        if (!partner.isActive()) {
            throw new IllegalArgumentException("Mobility partner " + partner.getId() + " is not active");
        }

        Ride ride = new Ride();
        ride.setLine(lines.findByCode(req.lineCode())
                .orElseThrow(() -> new LineNotFoundException(req.lineCode())));
        ride.setPartner(partner);
        if (req.brandCode() != null && !req.brandCode().isBlank()) {
            ride.setBrand(brands.findByCode(req.brandCode()));
        }
        ride.setDepartureAt(req.departureAt());
        ride.setArrivalAt(req.arrivalAt());
        ride.setCapacity(req.capacity());

        if (req.stops() != null) {
            for (int i = 0; i < req.stops().size(); i++) {
                var s = req.stops().get(i);
                ride.addStop(i + 1, s.stationName(), s.scheduledAt());
            }
        }

        Ride saved = rides.save(ride);
        events.publishEvent(RideEvent.of(RideEvent.Type.RIDE_SCHEDULED, saved));
        return saved;
    }

    @Transactional
    public Ride cancel(Long id) {
        Ride ride = findDetailed(id);
        ride.cancel();
        events.publishEvent(RideEvent.of(RideEvent.Type.RIDE_CANCELLED, ride));
        return ride;
    }

    /**
     * Re-emits the current state of every ride. Downstream consumers key by ride id,
     * so this is idempotent — useful to bootstrap the capacity-service with seed data.
     */
    @Transactional
    public int replayAll() {
        List<Ride> all = rides.search(null, null);
        all.forEach(r -> events.publishEvent(RideEvent.of(RideEvent.Type.RIDE_SNAPSHOT, r)));
        return all.size();
    }
}
