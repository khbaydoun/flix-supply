package com.khalil.supply.brand.dto;

import com.khalil.supply.brand.Brand;

public record BrandDto(Long id, String code, String name) {

    public static BrandDto from(Brand brand) {
        return new BrandDto(brand.getId(), brand.getCode(), brand.getName());
    }
}