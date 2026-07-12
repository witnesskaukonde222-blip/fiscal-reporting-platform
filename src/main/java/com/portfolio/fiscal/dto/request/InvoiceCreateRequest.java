package com.portfolio.fiscal.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceCreateRequest(
        @NotNull UUID customerId,
        String currency,
        LocalDate dueDate,
        @NotEmpty @Valid List<InvoiceLineItemRequest> lineItems
) {}
