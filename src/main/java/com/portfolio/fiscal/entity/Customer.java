package com.portfolio.fiscal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Getter
@Setter
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false)
    private String name;

    private String tin;

    @Column(name = "vat_number")
    private String vatNumber;

    @Column(name = "is_vat_registered", nullable = false)
    private boolean vatRegistered;

    private String email;
    private String phone;

    @Column(name = "address_line1")
    private String addressLine1;

    private String city;
    private String country;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
