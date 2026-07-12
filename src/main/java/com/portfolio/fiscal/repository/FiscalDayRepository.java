package com.portfolio.fiscal.repository;

import com.portfolio.fiscal.entity.FiscalDay;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface FiscalDayRepository extends JpaRepository<FiscalDay, UUID> {
    Optional<FiscalDay> findFirstByOrgIdAndStatusOrderByFiscalDayNoDesc(UUID orgId, String status);
    Optional<FiscalDay> findFirstByOrgIdOrderByFiscalDayNoDesc(UUID orgId);
}
