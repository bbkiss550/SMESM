package com.smeservicemanager.customer;

import com.smeservicemanager.shared.domain.DomainTypes.CustomerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CustomerForm {
    @NotNull private CustomerType customerType = CustomerType.INDIVIDUAL;
    @NotBlank private String name;
    private String companyName;
    private String taxId;
    @Email private String email;
    private String lineId;
    private String address;
    private String note;
    @NotBlank private String primaryPhone;
    private String secondaryPhone;
}
