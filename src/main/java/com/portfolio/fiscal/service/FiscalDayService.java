package com.portfolio.fiscal.service;

import com.portfolio.fiscal.entity.FiscalDay;
import com.portfolio.fiscal.repository.FiscalDayRepository;
import com.portfolio.fiscal.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Manages the FDMS-style fiscal day lifecycle. Every invoice must be issued
 * within an OPEN fiscal day; the receipt counter resets to 0 at the start of
 * each new fiscal day while the org-wide global_receipt_no never resets.
 */
@Service
public class FiscalDayService {

    private final FiscalDayRepository fiscalDayRepository;
    private final InvoiceRepository invoiceRepository;

    public FiscalDayService(FiscalDayRepository fiscalDayRepository, InvoiceRepository invoiceRepository) {
        this.fiscalDayRepository = fiscalDayRepository;
        this.invoiceRepository = invoiceRepository;
    }

    /** Returns the currently open fiscal day for the org, opening a new one if none is open. */
    @Transactional
    public FiscalDay getOrOpenCurrentFiscalDay(UUID orgId) {
        return fiscalDayRepository.findFirstByOrgIdAndStatusOrderByFiscalDayNoDesc(orgId, "OPEN")
                .orElseGet(() -> openNewFiscalDay(orgId));
    }

    @Transactional
    public FiscalDay openNewFiscalDay(UUID orgId) {
        int nextDayNo = fiscalDayRepository.findFirstByOrgIdOrderByFiscalDayNoDesc(orgId)
                .map(FiscalDay::getFiscalDayNo)
                .orElse(0) + 1;

        FiscalDay day = new FiscalDay();
        day.setOrgId(orgId);
        day.setFiscalDayNo(nextDayNo);
        day.setBusinessDate(LocalDate.now());
        day.setStatus("OPEN");
        day.setOpeningReceiptCtr(0);
        return fiscalDayRepository.save(day);
    }

    /**
     * Acquires a row-level lock on the fiscal day. Must be called inside the
     * same transaction as the counter reads that follow it, and before those
     * reads — otherwise two concurrent invoice creations can both read the
     * same "current max" counter and collide.
     */
    @Transactional
    public FiscalDay lockFiscalDay(UUID fiscalDayId) {
        return fiscalDayRepository.findByIdForUpdate(fiscalDayId)
                .orElseThrow(() -> new IllegalArgumentException("Fiscal day not found: " + fiscalDayId));
    }

    @Transactional
    public FiscalDay closeFiscalDay(UUID fiscalDayId) {
        FiscalDay day = fiscalDayRepository.findById(fiscalDayId)
                .orElseThrow(() -> new IllegalArgumentException("Fiscal day not found: " + fiscalDayId));

        int lastCounter = invoiceRepository.findFirstByFiscalDayIdOrderByReceiptCounterDesc(fiscalDayId)
                .map(inv -> inv.getReceiptCounter())
                .orElse(day.getOpeningReceiptCtr());

        day.setClosingReceiptCtr(lastCounter);
        day.setStatus("CLOSED");
        day.setClosedAt(java.time.Instant.now());
        return fiscalDayRepository.save(day);
    }

    /** Next per-fiscal-day receipt counter (resets each fiscal day). */
    public int nextReceiptCounter(UUID fiscalDayId, int openingCtr) {
        return invoiceRepository.findFirstByFiscalDayIdOrderByReceiptCounterDesc(fiscalDayId)
                .map(inv -> inv.getReceiptCounter() + 1)
                .orElse(openingCtr + 1);
    }

    /** Next org-wide global receipt number — never resets across fiscal days. */
    public long nextGlobalReceiptNo(UUID orgId) {
        return invoiceRepository.findFirstByOrgIdOrderByGlobalReceiptNoDesc(orgId)
                .map(inv -> inv.getGlobalReceiptNo() + 1)
                .orElse(1L);
    }
}