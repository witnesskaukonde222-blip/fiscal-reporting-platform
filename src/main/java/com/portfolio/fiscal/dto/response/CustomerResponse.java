package com.portfolio.fiscal.dto.response;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String tin,
        String vatNumber,
        boolean vatRegistered,
        String email,
        String phone,
        String city,
        String country
) {}
