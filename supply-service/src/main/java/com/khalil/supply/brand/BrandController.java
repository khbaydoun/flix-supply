package com.khalil.supply.brand;

import com.khalil.supply.brand.dto.BrandDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/brands")
public class BrandController {

    private final BrandService service;

    public BrandController(BrandService service) {
        this.service = service;
    }

    @GetMapping
    public List<BrandDto> findAll() {
        return service.findAll().stream()
                .map(BrandDto::from)
                .toList();
    }

    @GetMapping("/{code}")
    public BrandDto findByCode(@PathVariable String code) {
        return BrandDto.from(service.findByCode(code));
    }
}