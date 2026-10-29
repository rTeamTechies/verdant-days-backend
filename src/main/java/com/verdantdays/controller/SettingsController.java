package com.verdantdays.controller;

import com.verdantdays.model.BillSettings;
import com.verdantdays.model.dto.ApiResponse;
import com.verdantdays.service.BillSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private BillSettingsService billSettingsService;

    @GetMapping("/bill")
    public ResponseEntity<ApiResponse<BillSettings>> getBillSettings() {
        return ResponseEntity.ok(new ApiResponse<>("OK", billSettingsService.get(), 200));
    }

    @PutMapping("/bill")
    public ResponseEntity<ApiResponse<BillSettings>> saveBillSettings(@RequestBody BillSettings body) {
        return ResponseEntity.ok(new ApiResponse<>("OK", billSettingsService.save(body), 200));
    }
}
