package com.portfolio.fiscal.service;

import com.portfolio.fiscal.dto.request.VatReturnGenerateRequest;
import com.portfolio.fiscal.dto.response.VatReturnLineItemResponse;
import com.portfolio.fiscal.dto.response.VatReturnResponse;
import com.portfolio.fiscal.entity.*;
import com.portfolio.fiscal.exception.ResourceNotFoundException;
import com.portfolio.fiscal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates a VAT return by aggregating issued invoices (and their line
 * items) within a period, grouped by tax code. Input tax / purchases are
 * left at zero for phase 1 — this system currently only tracks sales-side
 * invoicing, not purchase invoices.
 */
@Service
public class VatReturnService {

    private final VatReturnRepository vatReturnRepository;
    private final VatReturnLineItemRepository vatReturnLineItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final TaxCodeRepository taxCodeRepository;
    private final AuditLogService auditLogService;

    public VatReturnService(VatReturnRepository vatReturnRepository,
                             VatReturnLineItemRepository vatReturnLineItemRepository,
                             InvoiceRepository invoiceRepository,
                             InvoiceLineItemRepository invoiceLineItemRepository,
                             TaxCodeRepository taxCodeRepository,
                             AuditLogService auditLogService) {
        this.vatReturnRepository = vatReturnRepository;
        this.vatReturnLineItemRepository = vatReturnLineItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.taxCodeRepository = taxCodeRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public VatReturnResponse generateReturn(UUID orgId, String actorSub, VatReturnGenerateRequest request) {
        VatReturn vatReturn = new VatReturn();
        vatReturn.setOrgId(orgId);
        vatReturn.setPeriodStart(request.periodStart());
        vatReturn.setPeriodEnd(request.periodEnd());
        vatReturn.setReturnType("VAT7");
        vatReturn.setStatus("DRAFT");

        // Pull invoices in range for this org
        var page = invoiceRepository.findByOrgId(orgId, org.springframework.data.domain.Pageable.unpaged());
        List<com.portfolio.fiscal.entity.Invoice> invoicesInPeriod = page.getContent().stream()
                .filter(inv -> !inv.getIssueDate().isBefore(request.periodStart())
                        && !inv.getIssueDate().isAfter(request.periodEnd())
                        && "ISSUED".equals(inv.getStatus()))
                .toList();

        Map<Short, BigDecimal> salesByTaxCode = new HashMap<>();
        Map<Short, BigDecimal> outputTaxByTaxCode = new HashMap<>();

        for (var invoice : invoicesInPeriod) {
            List<InvoiceLineItem> lines = invoiceLineItemRepository.findByInvoiceIdOrderByLineNo(invoice.getId());
            for (InvoiceLineItem line : lines) {
                salesByTaxCode.merge(line.getTaxCodeId(), line.getLineSubtotal(), BigDecimal::add);
                outputTaxByTaxCode.merge(line.getTaxCodeId(), line.getLineTax(), BigDecimal::add);
            }
        }

        BigDecimal outputTaxTotal = outputTaxByTaxCode.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        vatReturn.setOutputTaxTotal(outputTaxTotal);
        vatReturn.setInputTaxTotal(BigDecimal.ZERO);
        vatReturn.setNetVatPayable(outputTaxTotal);

        VatReturn saved = vatReturnRepository.save(vatReturn);

        List<VatReturnLineItem> lineItems = new ArrayList<>();
        for (Short taxCodeId : salesByTaxCode.keySet()) {
            VatReturnLineItem item = new VatReturnLineItem();
            item.setVatReturnId(saved.getId());
            item.setTaxCodeId(taxCodeId);
            item.setSalesTotal(salesByTaxCode.getOrDefault(taxCodeId, BigDecimal.ZERO));
            item.setOutputTax(outputTaxByTaxCode.getOrDefault(taxCodeId, BigDecimal.ZERO));
            item.setPurchasesTotal(BigDecimal.ZERO);
            item.setInputTax(BigDecimal.ZERO);
            lineItems.add(item);
        }
        vatReturnLineItemRepository.saveAll(lineItems);

        auditLogService.record(orgId, actorSub, "VAT_RETURN_GENERATED", "VAT_RETURN", saved.getId(),
                "{\"outputTaxTotal\":\"" + outputTaxTotal + "\"}");

        return toResponse(saved, lineItems);
    }

    @Transactional
    public VatReturnResponse submitReturn(UUID orgId, String actorSub, UUID vatReturnId, String submissionReference) {
        VatReturn vatReturn = vatReturnRepository.findByIdAndOrgId(vatReturnId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("VAT return not found: " + vatReturnId));

        vatReturn.setStatus("SUBMITTED");
        vatReturn.setSubmissionReference(submissionReference);
        vatReturn.setSubmittedBy(actorSub);
        vatReturn.setSubmittedAt(java.time.Instant.now());
        VatReturn saved = vatReturnRepository.save(vatReturn);

        auditLogService.record(orgId, actorSub, "VAT_RETURN_SUBMITTED", "VAT_RETURN", saved.getId(),
                "{\"submissionReference\":\"" + submissionReference + "\"}");

        List<VatReturnLineItem> lineItems = vatReturnLineItemRepository.findByVatReturnId(saved.getId());
        return toResponse(saved, lineItems);
    }

    @Transactional(readOnly = true)
    public VatReturnResponse getReturn(UUID orgId, UUID vatReturnId) {
        VatReturn vatReturn = vatReturnRepository.findByIdAndOrgId(vatReturnId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("VAT return not found: " + vatReturnId));
        List<VatReturnLineItem> lineItems = vatReturnLineItemRepository.findByVatReturnId(vatReturnId);
        return toResponse(vatReturn, lineItems);
    }

    private VatReturnResponse toResponse(VatReturn vatReturn, List<VatReturnLineItem> lineItems) {
        Map<Short, String> taxCodeNames = new HashMap<>();
        List<VatReturnLineItemResponse> lineResponses = lineItems.stream()
                .map(li -> new VatReturnLineItemResponse(
                        taxCodeNames.computeIfAbsent(li.getTaxCodeId(),
                                id -> taxCodeRepository.findById(id).map(TaxCode::getCode).orElse("?")),
                        li.getSalesTotal(), li.getOutputTax(), li.getPurchasesTotal(), li.getInputTax()))
                .collect(Collectors.toList());

        return new VatReturnResponse(
                vatReturn.getId(), vatReturn.getPeriodStart(), vatReturn.getPeriodEnd(), vatReturn.getReturnType(),
                vatReturn.getStatus(), vatReturn.getOutputTaxTotal(), vatReturn.getInputTaxTotal(),
                vatReturn.getNetVatPayable(), lineResponses
        );
    }
}
