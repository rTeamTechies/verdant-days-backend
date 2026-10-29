package com.verdantdays.controller;

import com.verdantdays.model.Expense;
import com.verdantdays.model.dto.ApiResponse;
import com.verdantdays.model.dto.ExpenseRequest;
import com.verdantdays.model.dto.IncomeExpenseSummary;
import com.verdantdays.model.dto.PageResponse;
import com.verdantdays.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<IncomeExpenseSummary>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(new ApiResponse<>("OK", expenseService.getSummary(startDate, endDate), 200));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Expense>>> getPage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(Math.max(1, size), 50);
        var pageResult = expenseService.getPage(startDate, endDate, page, size);
        return ResponseEntity.ok(new ApiResponse<>("OK", new PageResponse<>(
                pageResult.getContent(), pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements()), 200));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Expense>> create(@RequestBody ExpenseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("Created", expenseService.create(req), 201));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Expense>> update(@PathVariable Integer id, @RequestBody ExpenseRequest req) {
        return ResponseEntity.ok(new ApiResponse<>("OK", expenseService.update(id, req), 200));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        expenseService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Deleted", null, 200));
    }
}
