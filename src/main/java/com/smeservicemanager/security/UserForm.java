package com.smeservicemanager.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserForm {
    @NotBlank @Size(min=3,max=80) private String username;
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    private String phone;
    @Email private String email;
    @NotNull private Long roleId;
    @NotBlank @Size(min=8) private String password;
}
