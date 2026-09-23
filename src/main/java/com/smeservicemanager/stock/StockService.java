package com.smeservicemanager.stock;

import com.smeservicemanager.catalog.Product;
import com.smeservicemanager.catalog.ProductRepository;
import com.smeservicemanager.shared.domain.DomainTypes.StockTransactionType;
import com.smeservicemanager.shared.exception.BusinessException;
import com.smeservicemanager.shared.exception.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class StockService {
    private final ProductRepository products;
    private final StockTransactionRepository transactions;

    public StockService(ProductRepository products, StockTransactionRepository transactions) {
        this.products = products; this.transactions = transactions;
    }

    @Transactional
    public void adjust(Long productId, StockTransactionType type, BigDecimal quantity, String reference, String note) {
        if (quantity == null || quantity.signum() <= 0) throw new BusinessException("จำนวนต้องมากกว่า 0");
        Product product = products.findByIdForUpdate(productId).orElseThrow(() -> new ResourceNotFoundException("ไม่พบสินค้า"));
        BigDecimal before = product.getStockQty();
        BigDecimal after = switch (type) {
            case IN, RETURN -> before.add(quantity);
            case OUT -> before.subtract(quantity);
            case ADJUST -> quantity;
        };
        if (after.signum() < 0) throw new BusinessException("สต๊อกไม่เพียงพอ");
        product.setStockQty(after); products.save(product);
        StockTransaction tx = new StockTransaction(); tx.setProduct(product); tx.setTransactionType(type); tx.setQuantity(quantity);
        tx.setQuantityBefore(before); tx.setQuantityAfter(after); tx.setReference(reference); tx.setNote(note); transactions.save(tx);
    }
}
