package com.portfolio.fiscal.controller;

import com.portfolio.fiscal.config.CurrentUser;
import com.portfolio.fiscal.dto.request.InvoiceCreateRequest;
import com.portfolio.fiscal.dto.response.InvoiceResponse;
import com.portfolio.fiscal.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final CurrentUser currentUser;

    public InvoiceController(InvoiceService invoiceService, CurrentUser currentUser) {
        this.invoiceService = invoiceService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<InvoiceResponse> createInvoice(@AuthenticationPrincipal Jwt jwt,
                                                         @Valid @RequestBody InvoiceCreateRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(
                currentUser.orgId(jwt), currentUser.sub(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'INTERN')")
    public InvoiceResponse getInvoice(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invoiceId) {
        return invoiceService.getInvoice(currentUser.orgId(jwt), invoiceId);
    }
}