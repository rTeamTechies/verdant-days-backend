package com.verdantdays.model.dto;

public class CustomerRequest {
    private String name;
    private String phoneNumber;
    private String alternatePhone;
    private String email;
    private String address;
    private Integer apartmentId;
    private String flatNumber;
    private Boolean subscribedMonthly;

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
    public Integer getApartmentId() { return apartmentId; }
    public void setApartmentId(Integer apartmentId) { this.apartmentId = apartmentId; }
    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }
    public Boolean getSubscribedMonthly() { return subscribedMonthly; }
    public void setSubscribedMonthly(Boolean subscribedMonthly) { this.subscribedMonthly = subscribedMonthly; }
}
