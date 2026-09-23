package com.smeservicemanager.security;

import com.smeservicemanager.shared.exception.BusinessException;
import com.smeservicemanager.shared.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class UserManagementService {
    private static final String ALPHABET="ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private final UserRepository users; private final RoleRepository roles; private final PasswordEncoder encoder; private final SecureRandom random=new SecureRandom();
    public UserManagementService(UserRepository users,RoleRepository roles,PasswordEncoder encoder){this.users=users;this.roles=roles;this.encoder=encoder;}

    @Transactional public User create(UserForm form){
        if(users.findByUsernameIgnoreCase(form.getUsername()).isPresent())throw new BusinessException("ชื่อผู้ใช้งานนี้มีอยู่แล้ว");
        User u=new User();u.setUsername(form.getUsername().trim());u.setFirstName(form.getFirstName().trim());u.setLastName(form.getLastName().trim());
        u.setPhone(form.getPhone());u.setEmail(form.getEmail());u.setRole(roles.findById(form.getRoleId()).orElseThrow(() -> new ResourceNotFoundException("ไม่พบบทบาท")));
        u.setPassword(encoder.encode(form.getPassword()));return users.save(u);
    }

    @Transactional public void toggle(Long id){User u=users.findById(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบผู้ใช้งาน"));if("admin".equalsIgnoreCase(u.getUsername())&&u.isActive())throw new BusinessException("ไม่สามารถปิดใช้งานบัญชีผู้ดูแลหลัก");if(u.isActive())u.deactivate();else u.activate();}
    @Transactional public String resetPassword(Long id){User u=users.findById(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบผู้ใช้งาน"));String password=randomPassword();u.setPassword(encoder.encode(password));u.setForcePasswordChange(true);return password;}
    private String randomPassword(){StringBuilder value=new StringBuilder(10);for(int i=0;i<10;i++)value.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));return value.toString();}
}
