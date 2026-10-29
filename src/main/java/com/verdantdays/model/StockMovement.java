package com.verdantdays.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"createdAt", "active"})
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;  // positive = add, negative = deduct

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Column(name = "order_id")
    private Integer orderId;  // set when type=ORDER_SALE

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum MovementType { ADD, ORDER_SALE }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public LocalDate getMovementDate() { return movementDate; }
    public void setMovementDate(LocalDate movementDate) { this.movementDate = movementDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
