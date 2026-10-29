package com.verdantdays.repository;

import com.verdantdays.model.Apartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, Integer> {
    List<Apartment> findByActiveTrueOrderByNameAsc();
    Page<Apartment> findByActiveTrueOrderByNameAsc(Pageable pageable);

    @Query("SELECT a FROM Apartment a WHERE a.active = true AND " +
           "(:q IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(a.address, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(a.promoCode, '')) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY a.name ASC")
    Page<Apartment> searchActive(@Param("q") String q, Pageable pageable);

    Optional<Apartment> findByPromoCodeIgnoreCaseAndActiveTrue(String promoCode);

    Optional<Apartment> findFirstByNameIgnoreCaseAndActiveTrue(String name);
}
