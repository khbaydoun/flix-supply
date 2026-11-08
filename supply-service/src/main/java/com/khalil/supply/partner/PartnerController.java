package com.khalil.supply.partner;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/partners")
@Transactional(readOnly = true)
public class PartnerController {

    private final MobilityPartnerRepository repository;

    public PartnerController(MobilityPartnerRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PartnerDto> findAll() {
        return repository.findAll().stream().map(PartnerDto::from).toList();
    }

    @GetMapping("/{id}")
    public PartnerDto findById(@PathVariable Long id) {
        return repository.findById(id).map(PartnerDto::from)
                .orElseThrow(() -> new PartnerNotFoundException(id));
    }
}
