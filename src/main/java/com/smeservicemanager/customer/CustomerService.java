package com.smeservicemanager.customer;

import com.smeservicemanager.shared.domain.DomainTypes.CustomerType;
import com.smeservicemanager.shared.exception.BusinessException;
import com.smeservicemanager.shared.exception.ResourceNotFoundException;
import com.smeservicemanager.shared.service.DocumentNumberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    private final CustomerRepository customers;
    private final DocumentNumberService numbers;

    public CustomerService(CustomerRepository customers, DocumentNumberService numbers) {
        this.customers = customers; this.numbers = numbers;
    }

    @Transactional
    public Customer create(CustomerForm form) {
        if (form.getCustomerType() == CustomerType.BUSINESS && (form.getCompanyName() == null || form.getCompanyName().isBlank())) {
            throw new BusinessException("ลูกค้าธุรกิจต้องระบุชื่อบริษัท");
        }
        Customer customer = new Customer(); customer.setCustomerCode(numbers.nextCustomerCode());
        customer.setCustomerType(form.getCustomerType()); customer.setName(form.getName().trim());
        customer.setCompanyName(form.getCompanyName()); customer.setTaxId(form.getTaxId()); customer.setEmail(form.getEmail());
        customer.setLineId(form.getLineId()); customer.setAddress(form.getAddress()); customer.setNote(form.getNote());
        CustomerPhone primary = new CustomerPhone(); primary.setPhone(form.getPrimaryPhone()); primary.setLabel("มือถือ"); primary.setPrimaryPhone(true);
        customer.addPhone(primary);
        if (form.getSecondaryPhone() != null && !form.getSecondaryPhone().isBlank()) {
            CustomerPhone secondary = new CustomerPhone(); secondary.setPhone(form.getSecondaryPhone()); secondary.setLabel("สำรอง");
            customer.addPhone(secondary);
        }
        return customers.save(customer);
    }

    @Transactional
    public void toggle(Long id) {
        Customer customer = customers.findById(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบลูกค้า"));
        if (customer.isActive()) customer.deactivate(); else customer.activate();
    }
}
