package com.verdantdays.service;

import com.verdantdays.model.Apartment;
import com.verdantdays.model.DeliverySlot;
import com.verdantdays.model.dto.DeliverySlotRequest;
import com.verdantdays.repository.DeliverySlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DeliverySlotService {

    @Autowired
    private DeliverySlotRepository deliverySlotRepository;

    @Autowired
    private ApartmentService apartmentService;

    public List<DeliverySlot> list(Integer apartmentId) {
        apartmentService.getById(apartmentId);
        return deliverySlotRepository.findByApartmentIdAndActiveTrueOrderByDeliveryDateAsc(apartmentId);
    }

    public DeliverySlot create(Integer apartmentId, DeliverySlotRequest req) {
        if (req == null || req.getDeliveryDate() == null) throw new RuntimeException("Delivery date is required");
        if (req.getDeliveryDate().isBefore(LocalDate.now())) throw new RuntimeException("Delivery date cannot be in the past");
        Apartment apartment = apartmentService.getById(apartmentId);
        if (deliverySlotRepository.existsByApartmentIdAndDeliveryDateAndActiveTrue(apartmentId, req.getDeliveryDate())) {
            throw new RuntimeException("A drop is already scheduled on this date");
        }
        DeliverySlot slot = new DeliverySlot();
        slot.setApartment(apartment);
        slot.setDeliveryDate(req.getDeliveryDate());
        slot.setNotes(req.getNotes() != null ? req.getNotes().trim() : null);
        slot.setActive(true);
        return deliverySlotRepository.save(slot);
    }

    public void delete(Integer apartmentId, Integer slotId) {
        DeliverySlot slot = deliverySlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Delivery date not found"));
        if (slot.getApartment() == null || !apartmentId.equals(slot.getApartment().getId())) {
            throw new RuntimeException("Delivery date does not belong to this apartment");
        }
        slot.setActive(false);
        deliverySlotRepository.save(slot);
    }
}
