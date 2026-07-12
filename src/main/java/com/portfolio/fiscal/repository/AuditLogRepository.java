package com.portfolio.fiscal.repository;

import com.portfolio.fiscal.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Optional<AuditLog> findFirstByOrgIdOrderByIdDesc(UUID orgId);
}
