package com.smeservicemanager.customer;

import com.smeservicemanager.shared.domain.AuditableEntity;
import com.smeservicemanager.shared.domain.DomainTypes.CustomerType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "m_customer")
public class Customer extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_m_customer")
    private Long id;

    @Column(name = "c_customer_code", nullable = false, unique = true, length = 30)
    private String customerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "c_customer_type", nullable = false, length = 20)
    private CustomerType customerType = CustomerType.INDIVIDUAL;

    @Column(name = "c_name", nullable = false, length = 200)
    private String name;

    @Column(name = "c_company_name", length = 200)
    private String companyName;

    @Column(name = "c_tax_id", length = 30)
    private String taxId;

    @Column(name = "c_email", length = 180)
    private String email;

    @Column(name = "c_line_id", length = 100)
    private String lineId;

    @Column(name = "c_address", columnDefinition = "text")
    private String address;

    @Column(name = "c_note", columnDefinition = "text")
    private String note;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("primaryPhone DESC, id ASC")
    private List<CustomerPhone> phones = new ArrayList<>();

    public void addPhone(CustomerPhone phone) {
        phone.setCustomer(this);
        phones.add(phone);
    }

    public String getDisplayName() {
        return companyName == null || companyName.isBlank() ? name : companyName;
    }

    public String getPrimaryPhone() {
        return phones.stream().filter(CustomerPhone::isActive).map(CustomerPhone::getPhone).findFirst().orElse("-");
    }

    public String getSecondaryPhone() {
        return phones.stream().filter(CustomerPhone::isActive).filter(phone -> !phone.isPrimaryPhone())
                .map(CustomerPhone::getPhone).findFirst().orElse("");
    }
}
