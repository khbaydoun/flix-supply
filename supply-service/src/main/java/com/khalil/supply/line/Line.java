package com.khalil.supply.line;

import com.khalil.supply.brand.Brand;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A route such as N120 Aachen → Köln. */
@Entity
@Table(name = "line")
@Getter
@Setter
@NoArgsConstructor
public class Line {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    /** Legacy: brand at line level. Rides may override it, see {@code Ride#effectiveBrand()}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;
}
