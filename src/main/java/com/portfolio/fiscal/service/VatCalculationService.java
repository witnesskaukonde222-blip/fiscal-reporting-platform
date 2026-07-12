package com.portfolio.fiscal.service;

import com.portfolio.fiscal.entity.TaxCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * VAT is calculated exclusive-of-price at the line level, then summed —
 * never re-rounded at the invoice total. This avoids the classic rounding
 * drift that shows up when totals are recomputed from a rounded subtotal.
 */
@Service
public class VatCalculationService {

    private static final int MONEY_SCALE = 2;

    public record LineTax(BigDecimal lineSubtotal, BigDecimal lineTax, BigDecimal lineTotal) {}

    public LineTax calculateLine(BigDecimal quantity, BigDecimal unitPrice, TaxCode taxCode) {
        BigDecimal lineSubtotal = quantity.multiply(unitPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal rate = taxCode.getRatePercent().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal lineTax = lineSubtotal.multiply(rate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal lineTotal = lineSubtotal.add(lineTax);
        return new LineTax(lineSubtotal, lineTax, lineTotal);
    }
}
