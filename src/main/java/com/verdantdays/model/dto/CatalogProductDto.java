package com.verdantdays.model.dto;

import java.math.BigDecimal;

public class CatalogProductDto {
    private Integer id;
    private String oilType;
    private String size;
    private String displayName;
    private BigDecimal mrp;

    public CatalogProductDto() {}

    public CatalogProductDto(Integer id, String oilType, String size, String displayName, BigDecimal mrp) {
        this.id = id;
        this.oilType = oilType;
        this.size = size;
        this.displayName = displayName;
        this.mrp = mrp;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getOilType() { return oilType; }
    public void setOilType(String oilType) { this.oilType = oilType; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }
}
