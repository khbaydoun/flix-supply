package com.khalil.supply.partner;

public record PartnerDto(Long id, String name, String country, boolean active) {
    public static PartnerDto from(MobilityPartner p) {
        return new PartnerDto(p.getId(), p.getName(), p.getCountry(), p.isActive());
    }
}
