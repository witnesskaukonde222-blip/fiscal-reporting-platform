package com.portfolio.fiscal.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record VatReturnGenerateRequest(
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd
) {}
