package com.khalil.supply.partner;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A bus/train operator that runs rides on behalf of a brand. */
@Entity
@Table(name = "mobility_partner")
@Getter
@Setter
@NoArgsConstructor
public class MobilityPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private boolean active = true;
}
