package com.portfolio.fiscal.config;

import com.portfolio.fiscal.entity.Organization;
import com.portfolio.fiscal.exception.ResourceNotFoundException;
import com.portfolio.fiscal.repository.OrganizationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves identity off a SecureShield JWT. SecureShield's token has no
 * org_id claim, only sub (username) and tin_number. We match tin_number
 * against organizations.tin to find which org a request belongs to.
 */
@Component
public class CurrentUser {

    private final OrganizationRepository organizationRepository;

    public CurrentUser(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public String sub(Jwt jwt) {
        return jwt.getSubject();
    }

    public UUID orgId(Jwt jwt) {
        String tin = jwt.getClaimAsString("tin_number");
        if (tin == null || tin.isBlank()) {
            throw new IllegalStateException("JWT is missing required 'tin_number' claim");
        }
        Organization org = organizationRepository.findByTin(tin)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No organization registered for TIN: " + tin));
        return org.getId();
    }
}