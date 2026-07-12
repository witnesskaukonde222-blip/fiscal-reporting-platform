package com.portfolio.fiscal.service;

import com.portfolio.fiscal.dto.request.CustomerCreateRequest;
import com.portfolio.fiscal.dto.response.CustomerResponse;
import com.portfolio.fiscal.entity.Customer;
import com.portfolio.fiscal.exception.ResourceNotFoundException;
import com.portfolio.fiscal.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse create(UUID orgId, CustomerCreateRequest request) {
        Customer customer = new Customer();
        customer.setOrgId(orgId);
        customer.setName(request.name());
        customer.setTin(request.tin());
        customer.setVatNumber(request.vatNumber());
        customer.setVatRegistered(request.vatRegistered());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setAddressLine1(request.addressLine1());
        customer.setCity(request.city());
        customer.setCountry(request.country());
        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> listForOrg(UUID orgId) {
        return customerRepository.findByOrgId(orgId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID orgId, UUID customerId) {
        Customer customer = customerRepository.findByIdAndOrgId(customerId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        return toResponse(customer);
    }

    private CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getTin(), c.getVatNumber(),
                c.isVatRegistered(), c.getEmail(), c.getPhone(), c.getCity(), c.getCountry());
    }
}
