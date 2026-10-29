package com.verdantdays.controller;

import com.verdantdays.model.Apartment;
import com.verdantdays.model.dto.ApiResponse;
import com.verdantdays.model.dto.ApartmentDetailResponse;
import com.verdantdays.model.dto.PageResponse;
import com.verdantdays.service.ApartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    @Autowired
    private ApartmentService apartmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Apartment>>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(Math.max(1, size), 100);
        page = Math.max(0, page);
        var pageResult = apartmentService.getPage(q, page, size);
        return ResponseEntity.ok(new ApiResponse<>("OK", new PageResponse<>(
                pageResult.getContent(), pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements()), 200));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Apartment>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>("OK", apartmentService.getById(id), 200));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<ApiResponse<ApartmentDetailResponse>> getDetails(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>("OK", apartmentService.getDetails(id), 200));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Apartment>> create(@RequestBody Apartment apartment) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("Created", apartmentService.createOrUpdate(apartment), 201));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Apartment>> update(@PathVariable Integer id, @RequestBody Apartment apartment) {
        apartment.setId(id);
        return ResponseEntity.ok(new ApiResponse<>("OK", apartmentService.createOrUpdate(apartment), 200));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        apartmentService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Deleted", null, 200));
    }
}
