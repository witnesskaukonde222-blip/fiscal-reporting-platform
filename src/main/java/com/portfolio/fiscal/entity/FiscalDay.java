package com.portfolio.fiscal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fiscal_days")
@Getter
@Setter
public class FiscalDay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "fiscal_day_no", nullable = false)
    private Integer fiscalDayNo;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(nullable = false)
    private String status; // OPEN, CLOSED

    @Column(name = "opening_receipt_ctr", nullable = false)
    private Integer openingReceiptCtr;

    @Column(name = "closing_receipt_ctr")
    private Integer closingReceiptCtr;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @PrePersist
    void onCreate() {
        openedAt = Instant.now();
        if (status == null) status = "OPEN";
        if (openingReceiptCtr == null) openingReceiptCtr = 0;
    }
}
