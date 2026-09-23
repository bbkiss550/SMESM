package com.smeservicemanager.security;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController {
    private final UserRepository users;private final RoleRepository roles;private final UserManagementService service;
    public UserController(UserRepository users,RoleRepository roles,UserManagementService service){this.users=users;this.roles=roles;this.service=service;}
    @GetMapping public String list(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="0")int page,Model model){model.addAttribute("users",users.search(q,PageRequest.of(page,10)));model.addAttribute("roles",roles.findAll());model.addAttribute("userForm",new UserForm());model.addAttribute("q",q);return "user/list";}
    @PostMapping public String create(@Valid UserForm form,BindingResult result,RedirectAttributes r){if(result.hasErrors()){r.addFlashAttribute("errorMessage","กรุณาตรวจสอบข้อมูลผู้ใช้งาน");}else{service.create(form);r.addFlashAttribute("successMessage","สร้างผู้ใช้งานแล้ว");}return "redirect:/users";}
    @PostMapping("/{id}/toggle")public String toggle(@PathVariable Long id,RedirectAttributes r){service.toggle(id);r.addFlashAttribute("successMessage","อัปเดตสถานะผู้ใช้งานแล้ว");return "redirect:/users";}
    @PostMapping("/{id}/reset-password")public String reset(@PathVariable Long id,RedirectAttributes r){String password=service.resetPassword(id);r.addFlashAttribute("temporaryPassword",password);return "redirect:/users";}
}
