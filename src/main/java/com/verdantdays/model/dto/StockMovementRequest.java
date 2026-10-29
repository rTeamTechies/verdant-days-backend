package com.verdantdays.model.dto;

import java.time.LocalDate;

public class StockMovementRequest {
    private LocalDate movementDate;
    private String description;
    private Integer productId;
    private Integer quantity;

    public LocalDate getMovementDate() { return movementDate; }
    public void setMovementDate(LocalDate movementDate) { this.movementDate = movementDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
