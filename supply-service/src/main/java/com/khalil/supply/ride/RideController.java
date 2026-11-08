package com.khalil.supply.ride;

import com.khalil.supply.ride.dto.CreateRideRequest;
import com.khalil.supply.ride.dto.RideDetailDto;
import com.khalil.supply.ride.dto.RideDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rides")
public class RideController {

    private final RideService service;

    public RideController(RideService service) {
        this.service = service;
    }

    @GetMapping
    public List<RideDto> search(@RequestParam(required = false) String lineCode,
                                @RequestParam(required = false) String brandCode) {
        return service.search(lineCode, brandCode).stream().map(RideDto::from).toList();
    }

    @GetMapping("/{id}")
    public RideDetailDto findById(@PathVariable Long id) {
        return RideDetailDto.from(service.findDetailed(id));
    }

    @PostMapping
    public ResponseEntity<RideDetailDto> create(@Valid @RequestBody CreateRideRequest request) {
        Ride ride = service.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(ride.getId()).toUri();
        return ResponseEntity.created(location).body(RideDetailDto.from(ride));
    }

    @PostMapping("/{id}/cancel")
    public RideDto cancel(@PathVariable Long id) {
        return RideDto.from(service.cancel(id));
    }

    @PostMapping("/events/replay")
    public Map<String, Integer> replay() {
        return Map.of("published", service.replayAll());
    }
}
