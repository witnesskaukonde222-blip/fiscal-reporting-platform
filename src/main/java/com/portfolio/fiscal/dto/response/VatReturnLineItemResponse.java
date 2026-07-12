package com.portfolio.fiscal.dto.response;

import java.math.BigDecimal;

public record VatReturnLineItemResponse(
        String taxCode,
        BigDecimal salesTotal,
        BigDecimal outputTax,
        BigDecimal purchasesTotal,
        BigDecimal inputTax
) {}
