package com.verdantdays.service;

import com.verdantdays.model.Apartment;
import com.verdantdays.model.Customer;
import com.verdantdays.model.DeliverySlot;
import com.verdantdays.model.dto.ApartmentDetailResponse;
import com.verdantdays.repository.ApartmentRepository;
import com.verdantdays.repository.CustomerRepository;
import com.verdantdays.repository.DeliverySlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApartmentService {

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DeliverySlotRepository deliverySlotRepository;

    public List<Apartment> getAllActive() {
        return apartmentRepository.findByActiveTrueOrderByNameAsc();
    }

    public Page<Apartment> getPage(String search, int page, int size) {
        var p = PageRequest.of(page, size);
        if (search != null && !search.trim().isEmpty()) {
            return apartmentRepository.searchActive(search.trim(), p);
        }
        return apartmentRepository.findByActiveTrueOrderByNameAsc(p);
    }

    public Apartment getById(Integer id) {
        return apartmentRepository.findById(id).orElseThrow(() -> new RuntimeException("Apartment not found"));
    }

    public ApartmentDetailResponse getDetails(Integer id) {
        Apartment apt = getById(id);
        List<Customer> customers = customerRepository.findByApartmentIdAndActiveTrueOrderByNameAsc(id);
        List<DeliverySlot> slots = deliverySlotRepository.findByApartmentIdAndActiveTrueOrderByDeliveryDateAsc(id);
        return new ApartmentDetailResponse(apt, customers, slots);
    }

    public Apartment createOrUpdate(Apartment apartment) {
        String promo = apartment.getPromoCode();
        if (promo != null) {
            promo = promo.trim().toUpperCase().replaceAll("\\s+", "");
            promo = promo.isEmpty() ? null : promo;
        }
        if (promo != null) {
            final Integer editingId = apartment.getId();
            apartmentRepository.findByPromoCodeIgnoreCaseAndActiveTrue(promo)
                    .ifPresent(existing -> {
                        if (editingId == null || !existing.getId().equals(editingId)) {
                            throw new RuntimeException("Promo code already used by another apartment");
                        }
                    });
        }

        Apartment target;
        if (apartment.getId() != null) {
            // Merge onto managed entity so request body never wipes related customers
            target = getById(apartment.getId());
        } else {
            target = new Apartment();
            target.setActive(true);
        }
        if (apartment.getName() != null && !apartment.getName().isBlank()) {
            target.setName(apartment.getName().trim());
        } else if (target.getId() == null) {
            throw new RuntimeException("Apartment name is required");
        }
        if (apartment.getAddress() != null) target.setAddress(apartment.getAddress());
        if (apartment.getBlockTower() != null) target.setBlockTower(apartment.getBlockTower());
        if (apartment.getAdminName() != null) target.setAdminName(apartment.getAdminName());
        if (apartment.getAdminContact() != null) target.setAdminContact(apartment.getAdminContact());
        // Always apply promo from request (including clear-to-null when client sends empty/null intentionally via update)
        if (apartment.getId() != null || promo != null) {
            target.setPromoCode(promo);
        }
        return apartmentRepository.save(target);
    }

    public void delete(Integer id) {
        Apartment a = getById(id);
        a.setActive(false);
        apartmentRepository.save(a);
    }
}
