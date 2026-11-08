package com.khalil.supply.brand;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository repository;

    public BrandService(BrandRepository repository) {
        this.repository = repository;
    }

    public List<Brand> findAll() {
        return repository.findAll();
    }

    public Brand findByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new BrandNotFoundException(code));
    }
}
