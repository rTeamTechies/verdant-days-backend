package com.verdantdays.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Kept for website/stock compatibility; optional for admin-created products. */
    @Enumerated(EnumType.STRING)
    @Column(name = "oil_type", length = 20)
    private OilType oilType;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", length = 10)
    private Size size;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "mrp", precision = 10, scale = 2)
    private BigDecimal mrp;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum OilType { GROUNDNUT, SESAME, COCONUT }
    public enum Size { ML_500, L_1 }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public OilType getOilType() { return oilType; }
    public void setOilType(OilType oilType) { this.oilType = oilType; }
    public Size getSize() { return size; }
    public void setSize(Size size) { this.size = size; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
