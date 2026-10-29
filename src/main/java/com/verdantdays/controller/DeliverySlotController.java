package com.verdantdays.controller;

import com.verdantdays.model.DeliverySlot;
import com.verdantdays.model.dto.ApiResponse;
import com.verdantdays.model.dto.DeliverySlotRequest;
import com.verdantdays.service.DeliverySlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apartments/{apartmentId}/delivery-slots")
public class DeliverySlotController {

    @Autowired
    private DeliverySlotService deliverySlotService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliverySlot>>> list(@PathVariable Integer apartmentId) {
        return ResponseEntity.ok(new ApiResponse<>("OK", deliverySlotService.list(apartmentId), 200));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeliverySlot>> create(@PathVariable Integer apartmentId,
                                                           @RequestBody DeliverySlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Created", deliverySlotService.create(apartmentId, request), 201));
    }

    @DeleteMapping("/{slotId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer apartmentId, @PathVariable Integer slotId) {
        deliverySlotService.delete(apartmentId, slotId);
        return ResponseEntity.ok(new ApiResponse<>("Deleted", null, 200));
    }
}
