package com.portfolio.fiscal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VatReturnResponse(
        UUID id,
        LocalDate periodStart,
        LocalDate periodEnd,
        String returnType,
        String status,
        BigDecimal outputTaxTotal,
        BigDecimal inputTaxTotal,
        BigDecimal netVatPayable,
        List<VatReturnLineItemResponse> lineItems
) {}
