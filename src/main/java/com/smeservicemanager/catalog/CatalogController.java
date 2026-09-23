package com.smeservicemanager.catalog;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CatalogController {
    private final ServiceCatalogRepository services; private final ProductRepository products; private final CatalogService catalog;
    public CatalogController(ServiceCatalogRepository services, ProductRepository products, CatalogService catalog) { this.services=services;this.products=products;this.catalog=catalog; }

    @GetMapping("/services") public String services(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model model){
        model.addAttribute("services",services.findByServiceNameContainingIgnoreCaseOrServiceCodeContainingIgnoreCase(q,q,PageRequest.of(page,10)));
        model.addAttribute("q",q);model.addAttribute("serviceForm",new ServiceForm());return "catalog/services";}
    @PostMapping("/services") public String createService(@Valid ServiceForm form,BindingResult result,RedirectAttributes r){if(result.hasErrors()){r.addFlashAttribute("errorMessage","กรุณาตรวจสอบข้อมูลบริการ");}else{catalog.createService(form);r.addFlashAttribute("successMessage","เพิ่มบริการแล้ว");}return "redirect:/services";}
    @PostMapping("/services/{id}/toggle") public String toggleService(@PathVariable Long id,RedirectAttributes r){catalog.toggleService(id);r.addFlashAttribute("successMessage","อัปเดตสถานะแล้ว");return "redirect:/services";}

    @GetMapping("/products") public String products(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model model){
        model.addAttribute("products",products.search(q,PageRequest.of(page,10)));model.addAttribute("q",q);model.addAttribute("productForm",new ProductForm());return "catalog/products";}
    @PostMapping("/products") public String createProduct(@Valid ProductForm form,BindingResult result,RedirectAttributes r){if(result.hasErrors()){r.addFlashAttribute("errorMessage","กรุณาตรวจสอบข้อมูลสินค้า");}else{catalog.createProduct(form);r.addFlashAttribute("successMessage","เพิ่มสินค้าแล้ว");}return "redirect:/products";}
    @PostMapping("/products/{id}/toggle") public String toggleProduct(@PathVariable Long id,RedirectAttributes r){catalog.toggleProduct(id);r.addFlashAttribute("successMessage","อัปเดตสถานะแล้ว");return "redirect:/products";}
}
