package com.verdantdays.controller;

import com.verdantdays.model.dto.ApiResponse;
import com.verdantdays.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> stats = dashboardService.getStats(startDate, endDate);
        return ResponseEntity.ok(new ApiResponse<>("OK", stats, 200));
    }

    @GetMapping("/monthly-sales")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlySales(
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        if (y < 2000 || y > 2100) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("Invalid year", null, 400));
        }
        return ResponseEntity.ok(new ApiResponse<>("OK", dashboardService.getMonthlySales(y), 200));
    }
}
