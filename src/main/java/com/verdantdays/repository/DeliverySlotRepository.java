package com.verdantdays.repository;

import com.verdantdays.model.DeliverySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliverySlotRepository extends JpaRepository<DeliverySlot, Integer> {
    List<DeliverySlot> findByApartmentIdAndActiveTrueOrderByDeliveryDateAsc(Integer apartmentId);

    Optional<DeliverySlot> findFirstByApartmentIdAndActiveTrueAndDeliveryDateGreaterThanEqualOrderByDeliveryDateAsc(
            Integer apartmentId, LocalDate fromDate);

    boolean existsByApartmentIdAndDeliveryDateAndActiveTrue(Integer apartmentId, LocalDate deliveryDate);
}
