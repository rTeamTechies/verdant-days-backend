package com.verdantdays.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "phone_number", nullable = true, length = 15)
    private String phoneNumber;

    @Column(name = "alternate_phone", length = 15)
    private String alternatePhone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id")
    @JsonIgnore
    private Apartment apartment;

    @Column(name = "flat_number", length = 50)
    private String flatNumber;

    @Column(name = "subscribed_monthly", nullable = false)
    private boolean subscribedMonthly = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_reminder_sent_at")
    private LocalDateTime lastReminderSentAt;

    /** Read-only view of orders. Do not cascade — customer edits must never delete orders. */
    @OneToMany(mappedBy = "customer")
    @JsonIgnore
    private List<Order> orders = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAlternatePhone() { return alternatePhone; }
    public void setAlternatePhone(String alternatePhone) { this.alternatePhone = alternatePhone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Apartment getApartment() { return apartment; }
    public void setApartment(Apartment apartment) { this.apartment = apartment; }
    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }
    public boolean isSubscribedMonthly() { return subscribedMonthly; }
    public void setSubscribedMonthly(boolean subscribedMonthly) { this.subscribedMonthly = subscribedMonthly; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastReminderSentAt() { return lastReminderSentAt; }
    public void setLastReminderSentAt(LocalDateTime lastReminderSentAt) { this.lastReminderSentAt = lastReminderSentAt; }
    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }
}
