package com.verdantdays.model.dto;

import java.math.BigDecimal;

public class PromoPriceDto {
    private Integer productId;
    private String displayName;
    private BigDecimal mrp;
    private BigDecimal apartmentPrice;
    private BigDecimal youSave;

    public PromoPriceDto() {}

    public PromoPriceDto(Integer productId, String displayName, BigDecimal mrp, BigDecimal apartmentPrice, BigDecimal youSave) {
        this.productId = productId;
        this.displayName = displayName;
        this.mrp = mrp;
        this.apartmentPrice = apartmentPrice;
        this.youSave = youSave;
    }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }
    public BigDecimal getApartmentPrice() { return apartmentPrice; }
    public void setApartmentPrice(BigDecimal apartmentPrice) { this.apartmentPrice = apartmentPrice; }
    public BigDecimal getYouSave() { return youSave; }
    public void setYouSave(BigDecimal youSave) { this.youSave = youSave; }
}
