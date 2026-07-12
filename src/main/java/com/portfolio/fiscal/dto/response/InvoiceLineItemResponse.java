package com.portfolio.fiscal.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineItemResponse(
        UUID id,
        short lineNo,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String taxCode,
        BigDecimal lineSubtotal,
        BigDecimal lineTax,
        BigDecimal lineTotal
) {}
