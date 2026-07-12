package com.portfolio.fiscal.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CustomerCreateRequest(
        @NotBlank String name,
        String tin,
        String vatNumber,
        boolean vatRegistered,
        String email,
        String phone,
        String addressLine1,
        String city,
        String country
) {}
