package com.portfolio.fiscal.repository;

import com.portfolio.fiscal.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findByTin(String tin);
}
