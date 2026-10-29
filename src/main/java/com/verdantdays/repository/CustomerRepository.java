package com.verdantdays.repository;

import com.verdantdays.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    List<Customer> findByActiveTrueOrderByNameAsc();
    Optional<Customer> findFirstByPhoneNumberAndActiveTrueOrderByIdAsc(String phoneNumber);

    Optional<Customer> findFirstByNameIgnoreCaseAndApartmentIdAndActiveTrueOrderByIdAsc(String name, Integer apartmentId);

    Optional<Customer> findFirstByNameIgnoreCaseAndApartmentIsNullAndActiveTrueOrderByIdAsc(String name);
    List<Customer> findByApartmentIdAndActiveTrueOrderByNameAsc(Integer apartmentId);
    @Query("SELECT c FROM Customer c WHERE c.active = true AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR (c.phoneNumber IS NOT NULL AND c.phoneNumber LIKE CONCAT('%', :search, '%'))) ORDER BY c.name")
    List<Customer> searchByNameOrPhone(@Param("search") String search);

    @Query("SELECT c FROM Customer c WHERE c.active = true " +
           "AND (:apartmentId IS NULL OR c.apartment.id = :apartmentId) " +
           "AND (:subscribed IS NULL OR c.subscribedMonthly = :subscribed) " +
           "ORDER BY c.name")
    Page<Customer> findAllActive(@Param("apartmentId") Integer apartmentId, @Param("subscribed") Boolean subscribed, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.active = true " +
           "AND (:apartmentId IS NULL OR c.apartment.id = :apartmentId) " +
           "AND (:subscribed IS NULL OR c.subscribedMonthly = :subscribed) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR (c.phoneNumber IS NOT NULL AND c.phoneNumber LIKE CONCAT('%', :search, '%'))) " +
           "ORDER BY c.name")
    Page<Customer> findAllActiveWithSearch(@Param("apartmentId") Integer apartmentId, @Param("subscribed") Boolean subscribed,
            @Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.active = true " +
           "AND (:apartmentId IS NULL OR c.apartment.id = :apartmentId) " +
           "AND (:subscribed IS NULL OR c.subscribedMonthly = :subscribed)")
    long countAllActive(@Param("apartmentId") Integer apartmentId, @Param("subscribed") Boolean subscribed);
}
