package com.verdantdays.controller;

import com.verdantdays.model.Product;
import com.verdantdays.model.dto.ApiResponse;
import com.verdantdays.model.dto.PageResponse;
import com.verdantdays.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Product>>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(Math.max(1, size), 100);
        page = Math.max(0, page);
        var pageResult = productService.getPage(q, page, size);
        return ResponseEntity.ok(new ApiResponse<>("OK", new PageResponse<>(
                pageResult.getContent(), pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements()), 200));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>("OK", productService.getById(id), 200));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> create(@RequestBody Product product) {
        if (product.getDisplayName() == null || product.getDisplayName().isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("Name is required", null, 400));
        }
        if (product.getPrice() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("Selling price is required", null, 400));
        }
        if (product.getMrp() == null) {
            product.setMrp(product.getPrice());
        }
        product.setId(null);
        product.setActive(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("Created", productService.createOrUpdate(product), 201));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> update(@PathVariable Integer id, @RequestBody Product product) {
        Product existing = productService.getById(id);
        if (product.getDisplayName() != null && !product.getDisplayName().isBlank()) {
            existing.setDisplayName(product.getDisplayName().trim());
        }
        if (product.getDescription() != null) {
            existing.setDescription(product.getDescription());
        }
        if (product.getPrice() != null) {
            existing.setPrice(product.getPrice());
        }
        if (product.getMrp() != null) {
            existing.setMrp(product.getMrp());
        }
        if (product.getOilType() != null) {
            existing.setOilType(product.getOilType());
        }
        if (product.getSize() != null) {
            existing.setSize(product.getSize());
        }
        return ResponseEntity.ok(new ApiResponse<>("OK", productService.createOrUpdate(existing), 200));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        productService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Deleted", null, 200));
    }
}
