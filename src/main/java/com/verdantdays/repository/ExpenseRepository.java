package com.verdantdays.repository;

import com.verdantdays.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer> {

    @Query("SELECT e FROM Expense e WHERE (:startDate IS NULL OR e.expenseDate >= :startDate) AND (:endDate IS NULL OR e.expenseDate <= :endDate) ORDER BY e.expenseDate DESC, e.createdAt DESC")
    Page<Expense> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE (:startDate IS NULL OR e.expenseDate >= :startDate) AND (:endDate IS NULL OR e.expenseDate <= :endDate)")
    java.math.BigDecimal sumAmountByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
