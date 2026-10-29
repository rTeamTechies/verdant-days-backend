package com.verdantdays.repository;

import com.verdantdays.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByActiveTrueOrderByDisplayNameAsc();
    Page<Product> findByActiveTrueOrderByDisplayNameAsc(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(:q IS NULL OR LOWER(p.displayName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY p.displayName ASC")
    Page<Product> searchActive(@Param("q") String q, Pageable pageable);

    List<Product> findByActiveTrueOrderByOilTypeAscSizeAsc();
    Optional<Product> findByOilTypeAndSizeAndActiveTrue(Product.OilType oilType, Product.Size size);
}
