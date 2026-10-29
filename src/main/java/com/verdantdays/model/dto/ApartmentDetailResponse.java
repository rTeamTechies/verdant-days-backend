package com.verdantdays.model.dto;

import com.verdantdays.model.Apartment;
import com.verdantdays.model.Customer;
import com.verdantdays.model.DeliverySlot;

import java.util.List;

public class ApartmentDetailResponse {
    private Apartment apartment;
    private int customerCount;
    private List<Customer> customers;
    private List<DeliverySlot> deliverySlots;

    public ApartmentDetailResponse(Apartment apartment, List<Customer> customers, List<DeliverySlot> deliverySlots) {
        this.apartment = apartment;
        this.customers = customers;
        this.customerCount = customers != null ? customers.size() : 0;
        this.deliverySlots = deliverySlots;
    }

    public Apartment getApartment() { return apartment; }
    public void setApartment(Apartment apartment) { this.apartment = apartment; }
    public int getCustomerCount() { return customerCount; }
    public void setCustomerCount(int customerCount) { this.customerCount = customerCount; }
    public List<Customer> getCustomers() { return customers; }
    public void setCustomers(List<Customer> customers) { this.customers = customers; }
    public List<DeliverySlot> getDeliverySlots() { return deliverySlots; }
    public void setDeliverySlots(List<DeliverySlot> deliverySlots) { this.deliverySlots = deliverySlots; }
}
