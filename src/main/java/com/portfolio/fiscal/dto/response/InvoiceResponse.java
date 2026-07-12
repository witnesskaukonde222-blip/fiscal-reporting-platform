package com.portfolio.fiscal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        long globalReceiptNo,
        String status,
        UUID customerId,
        String currency,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal subtotal,
        BigDecimal taxTotal,
        BigDecimal total,
        String verificationCode,
        String qrPayload,
        List<InvoiceLineItemResponse> lineItems
) {}
