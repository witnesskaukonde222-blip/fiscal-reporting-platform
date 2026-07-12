package com.portfolio.fiscal.repository;

import com.portfolio.fiscal.entity.VatReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VatReturnRepository extends JpaRepository<VatReturn, UUID> {
    List<VatReturn> findByOrgIdOrderByPeriodStartDesc(UUID orgId);
    Optional<VatReturn> findByIdAndOrgId(UUID id, UUID orgId);
    Optional<VatReturn> findByOrgIdAndPeriodStartAndPeriodEndAndReturnType(
            UUID orgId, LocalDate periodStart, LocalDate periodEnd, String returnType);
}
