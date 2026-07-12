package com.portfolio.fiscal.controller;

import com.portfolio.fiscal.config.CurrentUser;
import com.portfolio.fiscal.dto.request.CustomerCreateRequest;
import com.portfolio.fiscal.dto.response.CustomerResponse;
import com.portfolio.fiscal.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CurrentUser currentUser;

    public CustomerController(CustomerService customerService, CurrentUser currentUser) {
        this.customerService = customerService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<CustomerResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody CustomerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.create(currentUser.orgId(jwt), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'INTERN')")
    public List<CustomerResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return customerService.listForOrg(currentUser.orgId(jwt));
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'INTERN')")
    public CustomerResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID customerId) {
        return customerService.get(currentUser.orgId(jwt), customerId);
    }
}