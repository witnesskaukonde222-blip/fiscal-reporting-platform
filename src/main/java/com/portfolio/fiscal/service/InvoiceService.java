package com.portfolio.fiscal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.fiscal.dto.request.InvoiceCreateRequest;
import com.portfolio.fiscal.dto.request.InvoiceLineItemRequest;
import com.portfolio.fiscal.dto.response.InvoiceLineItemResponse;
import com.portfolio.fiscal.dto.response.InvoiceResponse;
import com.portfolio.fiscal.entity.*;
import com.portfolio.fiscal.exception.ResourceNotFoundException;
import com.portfolio.fiscal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final CustomerRepository customerRepository;
    private final TaxCodeRepository taxCodeRepository;
    private final FiscalDayService fiscalDayService;
    private final HashChainService hashChainService;
    private final VatCalculationService vatCalculationService;
    private final QrCodeService qrCodeService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InvoiceService(InvoiceRepository invoiceRepository,
                           InvoiceLineItemRepository lineItemRepository,
                           CustomerRepository customerRepository,
                           TaxCodeRepository taxCodeRepository,
                           FiscalDayService fiscalDayService,
                           HashChainService hashChainService,
                           VatCalculationService vatCalculationService,
                           QrCodeService qrCodeService,
                           AuditLogService auditLogService) {
        this.invoiceRepository = invoiceRepository;
        this.lineItemRepository = lineItemRepository;
        this.customerRepository = customerRepository;
        this.taxCodeRepository = taxCodeRepository;
        this.fiscalDayService = fiscalDayService;
        this.hashChainService = hashChainService;
        this.vatCalculationService = vatCalculationService;
        this.qrCodeService = qrCodeService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public InvoiceResponse createInvoice(UUID orgId, String actorSub, InvoiceCreateRequest request) {
        Customer customer = customerRepository.findByIdAndOrgId(request.customerId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));

        FiscalDay fiscalDay = fiscalDayService.getOrOpenCurrentFiscalDay(orgId);

        Invoice invoice = new Invoice();
        invoice.setOrgId(orgId);
        invoice.setCustomerId(customer.getId());
        invoice.setFiscalDayId(fiscalDay.getId());
        invoice.setCurrency(request.currency() != null ? request.currency() : "USD");
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(request.dueDate());
        invoice.setCreatedBy(actorSub);
        invoice.setStatus("ISSUED");

        int receiptCounter = fiscalDayService.nextReceiptCounter(fiscalDay.getId(), fiscalDay.getOpeningReceiptCtr());
        long globalReceiptNo = fiscalDayService.nextGlobalReceiptNo(orgId);
        invoice.setReceiptCounter(receiptCounter);
        invoice.setGlobalReceiptNo(globalReceiptNo);
        invoice.setInvoiceNumber("INV-%05d".formatted(globalReceiptNo));

        // Build line items and totals
        List<InvoiceLineItem> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        short lineNo = 1;

        for (InvoiceLineItemRequest req : request.lineItems()) {
            TaxCode taxCode = taxCodeRepository.findByCode(req.taxCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Unknown tax code: " + req.taxCode()));

            VatCalculationService.LineTax computed = vatCalculationService.calculateLine(
                    req.quantity(), req.unitPrice(), taxCode);

            InvoiceLineItem item = new InvoiceLineItem();
            item.setLineNo(lineNo++);
            item.setDescription(req.description());
            item.setQuantity(req.quantity());
            item.setUnitPrice(req.unitPrice());
            item.setTaxCodeId(taxCode.getId());
            item.setLineSubtotal(computed.lineSubtotal());
            item.setLineTax(computed.lineTax());
            item.setLineTotal(computed.lineTotal());
            lineItems.add(item);

            subtotal = subtotal.add(computed.lineSubtotal());
            taxTotal = taxTotal.add(computed.lineTax());
        }

        invoice.setSubtotal(subtotal);
        invoice.setTaxTotal(taxTotal);
        invoice.setTotal(subtotal.add(taxTotal));

        // Hash chain: link to previous receipt within this fiscal day
        String previousHash = invoiceRepository
                .findFirstByFiscalDayIdOrderByReceiptCounterDesc(fiscalDay.getId())
                .map(Invoice::getReceiptHash)
                .orElse(null);

        String canonicalPayload = String.join("|",
                orgId.toString(), invoice.getInvoiceNumber(), invoice.getIssueDate().toString(),
                invoice.getTotal().toString(), String.valueOf(globalReceiptNo));
        String receiptHash = hashChainService.computeHash(canonicalPayload, previousHash);
        String verificationCode = hashChainService.shortVerificationCode(receiptHash);

        invoice.setPreviousReceiptHash(previousHash);
        invoice.setReceiptHash(receiptHash);
        invoice.setVerificationCode(verificationCode);
        invoice.setQrPayload(qrCodeService.buildPayload(orgId, invoice.getInvoiceNumber(),
                invoice.getIssueDate(), globalReceiptNo, verificationCode));

        Invoice saved = invoiceRepository.save(invoice);
        for (InvoiceLineItem item : lineItems) {
            item.setInvoiceId(saved.getId());
        }
        lineItemRepository.saveAll(lineItems);

        auditLogService.record(orgId, actorSub, "INVOICE_ISSUED", "INVOICE", saved.getId(),
                writeAuditPayload(saved));

        return toResponse(saved, lineItems);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID orgId, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndOrgId(invoiceId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        List<InvoiceLineItem> lineItems = lineItemRepository.findByInvoiceIdOrderByLineNo(invoiceId);
        return toResponse(invoice, lineItems);
    }

    private String writeAuditPayload(Invoice invoice) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("invoiceNumber", invoice.getInvoiceNumber());
            payload.put("total", invoice.getTotal());
            payload.put("globalReceiptNo", invoice.getGlobalReceiptNo());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    private InvoiceResponse toResponse(Invoice invoice, List<InvoiceLineItem> lineItems) {
        Map<Short, String> taxCodeNames = new HashMap<>();
        List<InvoiceLineItemResponse> lineResponses = lineItems.stream()
                .map(li -> new InvoiceLineItemResponse(
                        li.getId(), li.getLineNo(), li.getDescription(), li.getQuantity(), li.getUnitPrice(),
                        taxCodeNames.computeIfAbsent(li.getTaxCodeId(),
                                id -> taxCodeRepository.findById(id).map(TaxCode::getCode).orElse("?")),
                        li.getLineSubtotal(), li.getLineTax(), li.getLineTotal()))
                .toList();

        return new InvoiceResponse(
                invoice.getId(), invoice.getInvoiceNumber(), invoice.getGlobalReceiptNo(), invoice.getStatus(),
                invoice.getCustomerId(), invoice.getCurrency(), invoice.getIssueDate(), invoice.getDueDate(),
                invoice.getSubtotal(), invoice.getTaxTotal(), invoice.getTotal(),
                invoice.getVerificationCode(), invoice.getQrPayload(), lineResponses
        );
    }
}
