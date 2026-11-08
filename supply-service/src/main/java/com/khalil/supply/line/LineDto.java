package com.khalil.supply.line;

public record LineDto(Long id, String code, String origin, String destination, String brandCode) {
    public static LineDto from(Line l) {
        return new LineDto(l.getId(), l.getCode(), l.getOrigin(), l.getDestination(),
                l.getBrand() == null ? null : l.getBrand().getCode());
    }
}
