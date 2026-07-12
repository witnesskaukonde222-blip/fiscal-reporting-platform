package com.portfolio.fiscal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vat_returns")
@Getter
@Setter
public class VatReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "return_type", nullable = false)
    private String returnType = "VAT7";

    @Column(nullable = false)
    private String status = "DRAFT"; // DRAFT, SUBMITTED, ACCEPTED, REJECTED

    @Column(name = "output_tax_total", nullable = false)
    private BigDecimal outputTaxTotal = BigDecimal.ZERO;

    @Column(name = "input_tax_total", nullable = false)
    private BigDecimal inputTaxTotal = BigDecimal.ZERO;

    @Column(name = "net_vat_payable", nullable = false)
    private BigDecimal netVatPayable = BigDecimal.ZERO;

    @Column(name = "submission_reference")
    private String submissionReference;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
