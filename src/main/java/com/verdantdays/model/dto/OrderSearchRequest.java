package com.verdantdays.model.dto;

import java.time.LocalDate;

public class OrderSearchRequest {
    private Integer customerId;
    private Integer apartmentId;
    private String paymentType;
    private Integer productId;
    private LocalDate startDate;
    private LocalDate endDate;

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public Integer getApartmentId() { return apartmentId; }
    public void setApartmentId(Integer apartmentId) { this.apartmentId = apartmentId; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
