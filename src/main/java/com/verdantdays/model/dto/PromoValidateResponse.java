package com.verdantdays.model.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PromoValidateResponse {
    private boolean valid;
    private String apartmentName;
    private LocalDate deliveryDate;
    private String message;
    private List<PromoPriceDto> prices = new ArrayList<>();

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getApartmentName() { return apartmentName; }
    public void setApartmentName(String apartmentName) { this.apartmentName = apartmentName; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<PromoPriceDto> getPrices() { return prices; }
    public void setPrices(List<PromoPriceDto> prices) { this.prices = prices; }
}
