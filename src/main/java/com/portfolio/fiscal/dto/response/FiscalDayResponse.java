package com.portfolio.fiscal.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record FiscalDayResponse(
        UUID id,
        int fiscalDayNo,
        LocalDate businessDate,
        String status,
        int openingReceiptCtr,
        Integer closingReceiptCtr
) {}
