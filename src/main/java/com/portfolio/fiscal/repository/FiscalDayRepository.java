package com.portfolio.fiscal.repository;

import com.portfolio.fiscal.entity.FiscalDay;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FiscalDayRepository extends JpaRepository<FiscalDay, UUID> {
    Optional<FiscalDay> findFirstByOrgIdAndStatusOrderByFiscalDayNoDesc(UUID orgId, String status);
    Optional<FiscalDay> findFirstByOrgIdOrderByFiscalDayNoDesc(UUID orgId);

    /**
     * Locks the fiscal day row for the duration of the caller's transaction.
     * Any other transaction trying to lock the same row blocks until this
     * one commits — this is what makes receipt counter allocation atomic,
     * since the counter reads that follow only ever see fully-committed state.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FiscalDay f WHERE f.id = :id")
    Optional<FiscalDay> findByIdForUpdate(@Param("id") UUID id);
}