package com.portfolio.fiscal.repository;

import com.portfolio.fiscal.entity.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {
    List<InvoiceLineItem> findByInvoiceIdOrderByLineNo(UUID invoiceId);
}
