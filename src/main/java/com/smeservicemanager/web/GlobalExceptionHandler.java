package com.smeservicemanager.web;

import com.smeservicemanager.shared.exception.BusinessException;
import com.smeservicemanager.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public String business(BusinessException ex, HttpServletRequest request, RedirectAttributes redirect) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            try {
                String path = URI.create(referer).getRawPath();
                return "redirect:" + (path == null || path.isBlank() ? "/dashboard" : path);
            } catch (IllegalArgumentException ignored) {
                return "redirect:/dashboard";
            }
        }
        return "redirect:/error?message=" + ex.getMessage();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("status", 404); model.addAttribute("message", ex.getMessage()); return "error/error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpected(Exception ex, Model model) {
        model.addAttribute("status", 500); model.addAttribute("message", "เกิดข้อผิดพลาดที่ไม่คาดคิด กรุณาลองใหม่อีกครั้ง"); return "error/error";
    }
}
