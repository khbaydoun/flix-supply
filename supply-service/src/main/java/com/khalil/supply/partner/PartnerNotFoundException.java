package com.khalil.supply.partner;

import com.khalil.supply.web.NotFoundException;

public class PartnerNotFoundException extends NotFoundException {
    public PartnerNotFoundException(Long id) {
        super("Mobility partner not found: " + id);
    }
}
