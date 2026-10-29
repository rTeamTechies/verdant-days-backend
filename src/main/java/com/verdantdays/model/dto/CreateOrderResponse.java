package com.verdantdays.model.dto;

public class CreateOrderResponse {
    private Integer id;
    private String orderNumber;

    public CreateOrderResponse(Integer id, String orderNumber) {
        this.id = id;
        this.orderNumber = orderNumber;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
}
