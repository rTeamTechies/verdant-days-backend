package com.verdantdays.repository;

import com.verdantdays.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {

    @Query("SELECT sm FROM StockMovement sm LEFT JOIN FETCH sm.product WHERE (:productId IS NULL OR sm.product.id = :productId) AND (:startDate IS NULL OR sm.movementDate >= :startDate) AND (:endDate IS NULL OR sm.movementDate <= :endDate) ORDER BY sm.movementDate DESC, sm.createdAt DESC")
    Page<StockMovement> findByFilters(@Param("productId") Integer productId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT COALESCE(SUM(sm.quantity), 0) FROM StockMovement sm WHERE sm.product.id = :productId")
    Integer getBalanceByProduct(@Param("productId") Integer productId);
}
