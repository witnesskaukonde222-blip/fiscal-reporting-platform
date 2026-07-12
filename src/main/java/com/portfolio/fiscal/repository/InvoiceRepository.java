package com.portfolio.fiscal.repository;

import com.portfolio.fiscal.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice> findByOrgId(UUID orgId, Pageable pageable);
    Optional<Invoice> findByIdAndOrgId(UUID id, UUID orgId);
    Optional<Invoice> findFirstByFiscalDayIdOrderByReceiptCounterDesc(UUID fiscalDayId);
    Optional<Invoice> findFirstByOrgIdOrderByGlobalReceiptNoDesc(UUID orgId);
}
