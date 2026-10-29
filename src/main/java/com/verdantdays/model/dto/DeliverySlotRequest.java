package com.verdantdays.model.dto;

import java.time.LocalDate;

public class DeliverySlotRequest {
    private LocalDate deliveryDate;
    private String notes;

    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
