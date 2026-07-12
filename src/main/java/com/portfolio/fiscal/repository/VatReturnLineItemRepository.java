package com.portfolio.fiscal.repository;

import com.portfolio.fiscal.entity.VatReturnLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VatReturnLineItemRepository extends JpaRepository<VatReturnLineItem, UUID> {
    List<VatReturnLineItem> findByVatReturnId(UUID vatReturnId);
}
