package com.portfolio.fiscal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "fiscal_day_id", nullable = false)
    private UUID fiscalDayId;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "global_receipt_no", nullable = false)
    private Long globalReceiptNo;

    @Column(name = "receipt_counter", nullable = false)
    private Integer receiptCounter;

    @Column(nullable = false)
    private String status; // DRAFT, ISSUED, PAID, CANCELLED, CREDITED

    @Column(nullable = false)
    private String currency;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "previous_receipt_hash", columnDefinition = "CHAR(64)")
    private String previousReceiptHash;

    @Column(name = "receipt_hash", nullable = false)
    private String receiptHash;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "qr_payload", columnDefinition = "TEXT")
    private String qrPayload;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "invoiceId", fetch = FetchType.LAZY)
    @Transient
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) status = "DRAFT";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
