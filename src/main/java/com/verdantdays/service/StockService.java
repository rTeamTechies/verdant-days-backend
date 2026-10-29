package com.verdantdays.service;

import com.verdantdays.model.Order;
import com.verdantdays.model.Product;
import com.verdantdays.model.StockMovement;
import com.verdantdays.model.dto.ProductStockBalance;
import com.verdantdays.model.dto.StockMovementRequest;
import com.verdantdays.repository.ProductRepository;
import com.verdantdays.repository.StockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockService {

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private ProductRepository productRepository;

    public Page<StockMovement> getPage(Integer productId, LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable p = PageRequest.of(page, Math.min(size, 50));
        return stockMovementRepository.findByFilters(productId, startDate, endDate, p);
    }

    public Integer getBalance(Integer productId) {
        Integer b = stockMovementRepository.getBalanceByProduct(productId);
        return b != null ? b : 0;
    }

    public List<ProductStockBalance> getAllBalances() {
        List<Product> products = productRepository.findByActiveTrueOrderByOilTypeAscSizeAsc();
        List<ProductStockBalance> result = new ArrayList<>();
        for (Product p : products) {
            result.add(new ProductStockBalance(p, getBalance(p.getId())));
        }
        return result;
    }

    @Transactional
    public StockMovement addStock(StockMovementRequest req) {
        if (req.getProductId() == null) throw new RuntimeException("productId is required");
        if (req.getQuantity() == null || req.getQuantity() <= 0) throw new RuntimeException("quantity must be positive");
        Product product = productRepository.findById(req.getProductId()).orElseThrow(() -> new RuntimeException("Product not found"));
        StockMovement sm = new StockMovement();
        sm.setMovementDate(req.getMovementDate() != null ? req.getMovementDate() : LocalDate.now());
        sm.setDescription(req.getDescription() != null ? req.getDescription().trim() : "");
        sm.setProduct(product);
        sm.setQuantity(req.getQuantity());
        sm.setMovementType(StockMovement.MovementType.ADD);
        return stockMovementRepository.save(sm);
    }

    @Transactional
    public void deductForOrder(Order order) {
        for (var item : order.getItems()) {
            createOrderDeduction(order, item.getProduct(), -item.getQuantity());
        }
    }

    @Transactional
    public void reverseOrderDeduction(Order order) {
        for (var item : order.getItems()) {
            createOrderDeduction(order, item.getProduct(), item.getQuantity());
        }
    }

    private void createOrderDeduction(Order order, Product product, int quantity) {
        StockMovement sm = new StockMovement();
        sm.setMovementDate(LocalDate.now());
        sm.setDescription("Order #" + (order.getOrderNumber() != null ? order.getOrderNumber() : order.getId()));
        sm.setProduct(product);
        sm.setQuantity(quantity);
        sm.setMovementType(StockMovement.MovementType.ORDER_SALE);
        sm.setOrderId(order.getId());
        stockMovementRepository.save(sm);
    }

    public void delete(Integer id) {
        stockMovementRepository.deleteById(id);
    }
}
