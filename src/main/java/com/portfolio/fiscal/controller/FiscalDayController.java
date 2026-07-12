package com.portfolio.fiscal.controller;

import com.portfolio.fiscal.config.CurrentUser;
import com.portfolio.fiscal.dto.response.FiscalDayResponse;
import com.portfolio.fiscal.entity.FiscalDay;
import com.portfolio.fiscal.service.FiscalDayService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fiscal-days")
public class FiscalDayController {

    private final FiscalDayService fiscalDayService;
    private final CurrentUser currentUser;

    public FiscalDayController(FiscalDayService fiscalDayService, CurrentUser currentUser) {
        this.fiscalDayService = fiscalDayService;
        this.currentUser = currentUser;
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'INTERN')")
    public FiscalDayResponse current(@AuthenticationPrincipal Jwt jwt) {
        return toResponse(fiscalDayService.getOrOpenCurrentFiscalDay(currentUser.orgId(jwt)));
    }

    @PostMapping("/{fiscalDayId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public FiscalDayResponse close(@PathVariable UUID fiscalDayId) {
        return toResponse(fiscalDayService.closeFiscalDay(fiscalDayId));
    }

    private FiscalDayResponse toResponse(FiscalDay day) {
        return new FiscalDayResponse(day.getId(), day.getFiscalDayNo(), day.getBusinessDate(),
                day.getStatus(), day.getOpeningReceiptCtr(), day.getClosingReceiptCtr());
    }
}