package com.portfolio.fiscal.controller;

import com.portfolio.fiscal.config.CurrentUser;
import com.portfolio.fiscal.dto.request.VatReturnGenerateRequest;
import com.portfolio.fiscal.dto.response.VatReturnResponse;
import com.portfolio.fiscal.service.VatReturnService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vat-returns")
public class VatReturnController {

    private final VatReturnService vatReturnService;
    private final CurrentUser currentUser;

    public VatReturnController(VatReturnService vatReturnService, CurrentUser currentUser) {
        this.vatReturnService = vatReturnService;
        this.currentUser = currentUser;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<VatReturnResponse> generate(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody VatReturnGenerateRequest request) {
        VatReturnResponse response = vatReturnService.generateReturn(
                currentUser.orgId(jwt), currentUser.sub(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{vatReturnId}/submit")
    @PreAuthorize("hasRole('ADMIN')")
    public VatReturnResponse submit(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable UUID vatReturnId,
                                    @RequestParam(required = false) String submissionReference) {
        return vatReturnService.submitReturn(currentUser.orgId(jwt), currentUser.sub(jwt),
                vatReturnId, submissionReference);
    }

    @GetMapping("/{vatReturnId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'INTERN')")
    public VatReturnResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID vatReturnId) {
        return vatReturnService.getReturn(currentUser.orgId(jwt), vatReturnId);
    }
}