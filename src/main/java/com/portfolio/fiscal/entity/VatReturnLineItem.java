package com.portfolio.fiscal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "vat_return_line_items")
@Getter
@Setter
public class VatReturnLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vat_return_id", nullable = false)
    private UUID vatReturnId;

    @Column(name = "tax_code_id", nullable = false)
    private Short taxCodeId;

    @Column(name = "sales_total", nullable = false)
    private BigDecimal salesTotal = BigDecimal.ZERO;

    @Column(name = "output_tax", nullable = false)
    private BigDecimal outputTax = BigDecimal.ZERO;

    @Column(name = "purchases_total", nullable = false)
    private BigDecimal purchasesTotal = BigDecimal.ZERO;

    @Column(name = "input_tax", nullable = false)
    private BigDecimal inputTax = BigDecimal.ZERO;
}
