package com.verdantdays.repository;

import com.verdantdays.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer>, JpaSpecificationExecutor<Order> {
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.customer.id = :customerId ORDER BY o.createdAt DESC")
    List<Order> findByCustomerIdWithItems(@Param("customerId") Integer customerId);
    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Integer id);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.createdAt BETWEEN :start AND :end ORDER BY o.createdAt DESC")
    List<Order> findByCreatedAtBetweenWithItems(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query(value = "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product " +
           "WHERE (:customerId IS NULL OR o.customer.id = :customerId) " +
           "AND (:apartmentId IS NULL OR o.customer.apartment.id = :apartmentId) " +
           "AND (:orderType IS NULL OR o.orderType = :orderType) " +
           "AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus) " +
           "AND (:paymentType IS NULL OR o.paymentType = :paymentType) " +
           "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
           "AND (:productId IS NULL OR EXISTS (SELECT 1 FROM OrderItem oi WHERE oi.order = o AND oi.product.id = :productId)) " +
           "AND (:orderNumber IS NULL OR :orderNumber = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) " +
           "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
           "ORDER BY o.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT o) FROM Order o " +
           "WHERE (:customerId IS NULL OR o.customer.id = :customerId) " +
           "AND (:apartmentId IS NULL OR o.customer.apartment.id = :apartmentId) " +
           "AND (:orderType IS NULL OR o.orderType = :orderType) " +
           "AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus) " +
           "AND (:paymentType IS NULL OR o.paymentType = :paymentType) " +
           "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
           "AND (:productId IS NULL OR EXISTS (SELECT 1 FROM OrderItem oi WHERE oi.order = o AND oi.product.id = :productId)) " +
           "AND (:orderNumber IS NULL OR :orderNumber = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) " +
           "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR o.createdAt <= :endDate)")
    org.springframework.data.domain.Page<Order> searchWithItemsPage(
            @Param("customerId") Integer customerId,
            @Param("apartmentId") Integer apartmentId,
            @Param("orderType") com.verdantdays.model.Order.OrderType orderType,
            @Param("orderStatus") com.verdantdays.model.Order.OrderStatus orderStatus,
            @Param("paymentType") com.verdantdays.model.Order.PaymentType paymentType,
            @Param("paymentStatus") com.verdantdays.model.Order.PaymentStatus paymentStatus,
            @Param("productId") Integer productId,
            @Param("orderNumber") String orderNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product " +
           "WHERE (:customerId IS NULL OR o.customer.id = :customerId) " +
           "AND (:apartmentId IS NULL OR o.customer.apartment.id = :apartmentId) " +
           "AND (:orderType IS NULL OR o.orderType = :orderType) " +
           "AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus) " +
           "AND (:paymentType IS NULL OR o.paymentType = :paymentType) " +
           "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
           "AND (:productId IS NULL OR EXISTS (SELECT 1 FROM OrderItem oi WHERE oi.order = o AND oi.product.id = :productId)) " +
           "AND (:orderNumber IS NULL OR :orderNumber = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) " +
           "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
           "ORDER BY o.createdAt DESC")
    List<Order> searchWithItems(
            @Param("customerId") Integer customerId,
            @Param("apartmentId") Integer apartmentId,
            @Param("orderType") com.verdantdays.model.Order.OrderType orderType,
            @Param("orderStatus") com.verdantdays.model.Order.OrderStatus orderStatus,
            @Param("paymentType") com.verdantdays.model.Order.PaymentType paymentType,
            @Param("paymentStatus") com.verdantdays.model.Order.PaymentStatus paymentStatus,
            @Param("productId") Integer productId,
            @Param("orderNumber") String orderNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o.customer.id, MAX(o.createdAt) FROM Order o GROUP BY o.customer.id")
    List<Object[]> findLastOrderDateByCustomer();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :start AND o.createdAt <= :end")
    java.math.BigDecimal sumTotalAmountByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT MONTH(o.createdAt), COALESCE(SUM(o.totalAmount), 0), COUNT(o) " +
           "FROM Order o WHERE YEAR(o.createdAt) = :year GROUP BY MONTH(o.createdAt) ORDER BY MONTH(o.createdAt)")
    List<Object[]> sumSalesByMonthForYear(@Param("year") int year);

    /** Order numbers for a calendar month: YYMM + any day + "-" + seq (e.g. 2610%). */
    @Query("SELECT o.orderNumber FROM Order o WHERE o.orderNumber IS NOT NULL AND o.orderNumber LIKE CONCAT(:ymPrefix, '%')")
    List<String> findOrderNumbersByYearMonthPrefix(@Param("ymPrefix") String ymPrefix);

    boolean existsByOrderNumber(String orderNumber);
}
