package com.portfolio.fiscal.repository;

import com.portfolio.fiscal.entity.TaxCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaxCodeRepository extends JpaRepository<TaxCode, Short> {
    Optional<TaxCode> findByCode(String code);
    List<TaxCode> findByActiveTrue();
}
