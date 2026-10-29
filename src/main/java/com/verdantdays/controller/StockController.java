package com.verdantdays.controller;

import com.verdantdays.model.StockMovement;
import com.verdantdays.model.dto.ApiResponse;
import com.verdantdays.model.dto.PageResponse;
import com.verdantdays.model.dto.ProductStockBalance;
import com.verdantdays.model.dto.StockMovementRequest;
import com.verdantdays.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    @Autowired
    private StockService stockService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StockMovement>>> getPage(
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(Math.max(1, size), 50);
        var pageResult = stockService.getPage(productId, startDate, endDate, page, size);
        return ResponseEntity.ok(new ApiResponse<>("OK", new PageResponse<>(
                pageResult.getContent(), pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements()), 200));
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<List<ProductStockBalance>>> getBalances() {
        return ResponseEntity.ok(new ApiResponse<>("OK", stockService.getAllBalances(), 200));
    }

    @GetMapping("/balance/{productId}")
    public ResponseEntity<ApiResponse<Integer>> getBalance(@PathVariable Integer productId) {
        return ResponseEntity.ok(new ApiResponse<>("OK", stockService.getBalance(productId), 200));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StockMovement>> addStock(@RequestBody StockMovementRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("Created", stockService.addStock(req), 201));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        stockService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Deleted", null, 200));
    }
}
