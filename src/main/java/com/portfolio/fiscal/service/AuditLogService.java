package com.portfolio.fiscal.service;

import com.portfolio.fiscal.entity.AuditLog;
import com.portfolio.fiscal.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Fiscal-domain audit trail (invoice issuance, VAT submission, etc).
 * SecureShield remains system-of-record for auth/security events; this is a
 * separate hash chain scoped to this service's own business events.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final HashChainService hashChainService;

    public AuditLogService(AuditLogRepository auditLogRepository, HashChainService hashChainService) {
        this.auditLogRepository = auditLogRepository;
        this.hashChainService = hashChainService;
    }

    public void record(UUID orgId, String actorSub, String action, String entityType, UUID entityId, String payloadJson) {
        String previousHash = auditLogRepository.findFirstByOrgIdOrderByIdDesc(orgId)
                .map(AuditLog::getEntryHash)
                .orElse(null);

        String canonical = orgId + "|" + actorSub + "|" + action + "|" + entityType + "|" + entityId + "|" + payloadJson;
        String entryHash = hashChainService.computeHash(canonical, previousHash);

        AuditLog entry = new AuditLog();
        entry.setOrgId(orgId);
        entry.setActorSub(actorSub);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setPayloadJson(payloadJson);
        entry.setPreviousHash(previousHash);
        entry.setEntryHash(entryHash);

        auditLogRepository.save(entry);
    }
}
