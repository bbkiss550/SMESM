package com.smeservicemanager.stock;

import com.smeservicemanager.catalog.ProductRepository;
import com.smeservicemanager.shared.domain.DomainTypes.StockTransactionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/stock")
public class StockController {
    private final StockTransactionRepository transactions; private final ProductRepository products; private final StockService service;
    public StockController(StockTransactionRepository transactions,ProductRepository products,StockService service){this.transactions=transactions;this.products=products;this.service=service;}

    @GetMapping public String index(@RequestParam(defaultValue="0") int page,Model model){
        model.addAttribute("transactions",transactions.findAll(PageRequest.of(page,10,Sort.by(Sort.Direction.DESC,"createDate"))));
        model.addAttribute("products",products.findByStatusOrderByProductNameAsc("A"));model.addAttribute("types",StockTransactionType.values());return "stock/index";}
    @PostMapping("/adjust") public String adjust(@RequestParam Long productId,@RequestParam StockTransactionType type,@RequestParam BigDecimal quantity,
                                                  @RequestParam(required=false) String reference,@RequestParam(required=false) String note,RedirectAttributes r){
        service.adjust(productId,type,quantity,reference,note);r.addFlashAttribute("successMessage","บันทึกรายการสต๊อกแล้ว");return "redirect:/stock";}
}
