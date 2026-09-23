package com.smeservicemanager.catalog;

import com.smeservicemanager.shared.exception.ResourceNotFoundException;
import com.smeservicemanager.stock.StockTransaction;
import com.smeservicemanager.stock.StockTransactionRepository;
import com.smeservicemanager.shared.domain.DomainTypes.StockTransactionType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final ServiceCatalogRepository services;
    private final ProductRepository products;
    private final StockTransactionRepository transactions;
    private final JdbcClient jdbc;

    public CatalogService(ServiceCatalogRepository services, ProductRepository products, StockTransactionRepository transactions, JdbcClient jdbc) {
        this.services = services; this.products = products; this.transactions = transactions; this.jdbc = jdbc;
    }

    @Transactional
    public ServiceCatalog createService(ServiceForm form) {
        long next = jdbc.sql("select coalesce(max(id_m_service),0)+1 from m_service").query(Long.class).single();
        ServiceCatalog entity = new ServiceCatalog(); entity.setServiceCode("SRV-%03d".formatted(next));
        entity.setServiceName(form.getServiceName()); entity.setDescription(form.getDescription()); entity.setDefaultPrice(form.getDefaultPrice());
        entity.setDefaultDurationMinutes(form.getDefaultDurationMinutes()); return services.save(entity);
    }

    @Transactional
    public Product createProduct(ProductForm form) {
        long next = jdbc.sql("select coalesce(max(id_m_product),0)+1 from m_product").query(Long.class).single();
        Product p = new Product(); p.setProductCode("P-%03d".formatted(next)); p.setProductName(form.getProductName());
        p.setCategory(form.getCategory()); p.setUnit(form.getUnit()); p.setCostPrice(form.getCostPrice()); p.setSalePrice(form.getSalePrice());
        p.setStockQty(form.getOpeningStock()); p.setMinStock(form.getMinStock()); products.save(p);
        if (form.getOpeningStock().signum() > 0) {
            StockTransaction tx = new StockTransaction(); tx.setProduct(p); tx.setTransactionType(StockTransactionType.IN);
            tx.setQuantity(form.getOpeningStock()); tx.setQuantityBefore(java.math.BigDecimal.ZERO); tx.setQuantityAfter(form.getOpeningStock());
            tx.setReference("OPENING"); tx.setNote("ยอดตั้งต้น"); transactions.save(tx);
        }
        return p;
    }

    @Transactional
    public void toggleService(Long id) { ServiceCatalog s=services.findById(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบบริการ")); if(s.isActive())s.deactivate();else s.activate(); }
    @Transactional
    public void toggleProduct(Long id) { Product p=products.findById(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบสินค้า")); if(p.isActive())p.deactivate();else p.activate(); }
}
