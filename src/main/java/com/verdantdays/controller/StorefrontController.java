package com.verdantdays.controller;

import com.verdantdays.model.dto.ApiResponse;
import com.verdantdays.model.dto.CatalogProductDto;
import com.verdantdays.model.dto.CheckoutRequest;
import com.verdantdays.model.dto.CheckoutResponse;
import com.verdantdays.model.dto.PromoValidateResponse;
import com.verdantdays.service.StorefrontService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class StorefrontController {

    @Autowired
    private StorefrontService storefrontService;

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<List<CatalogProductDto>>> catalog() {
        return ResponseEntity.ok(new ApiResponse<>("OK", storefrontService.catalog(), 200));
    }

    @PostMapping("/promo/validate")
    public ResponseEntity<ApiResponse<PromoValidateResponse>> validatePromo(@RequestBody Map<String, String> body) {
        String code = body != null ? body.get("code") : null;
        PromoValidateResponse res = storefrontService.validatePromo(code);
        return ResponseEntity.ok(new ApiResponse<>(res.isValid() ? "OK" : res.getMessage(), res, 200));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(@RequestBody CheckoutRequest request) {
        CheckoutResponse res = storefrontService.checkout(request);
        return ResponseEntity.ok(new ApiResponse<>("Created", res, 200));
    }
}
