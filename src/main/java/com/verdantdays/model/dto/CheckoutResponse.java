package com.verdantdays.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CheckoutResponse {
    private Integer orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private LocalDate deliveryDate;
    private String fulfillmentType;
    private String apartmentName;
    private String message;

    public CheckoutResponse() {}

    public CheckoutResponse(Integer orderId, String orderNumber, BigDecimal totalAmount, LocalDate deliveryDate,
                            String fulfillmentType, String apartmentName, String message) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.deliveryDate = deliveryDate;
        this.fulfillmentType = fulfillmentType;
        this.apartmentName = apartmentName;
        this.message = message;
    }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getFulfillmentType() { return fulfillmentType; }
    public void setFulfillmentType(String fulfillmentType) { this.fulfillmentType = fulfillmentType; }
    public String getApartmentName() { return apartmentName; }
    public void setApartmentName(String apartmentName) { this.apartmentName = apartmentName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
