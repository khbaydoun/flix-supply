package com.khalil.supply.brand;

import com.khalil.supply.web.NotFoundException;

public class BrandNotFoundException extends NotFoundException {
    public BrandNotFoundException(String code) {
        super("Brand not found: " + code);
    }
}
