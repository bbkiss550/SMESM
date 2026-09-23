package com.smeservicemanager.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/") public String landing() { return "public/landing"; }
    @GetMapping("/login") public String login() { return "public/login"; }
    @GetMapping("/error/403") public String forbidden(Model model) {
        model.addAttribute("status", 403); model.addAttribute("message", "คุณไม่มีสิทธิ์เข้าถึงหน้านี้"); return "error/error";
    }
}
