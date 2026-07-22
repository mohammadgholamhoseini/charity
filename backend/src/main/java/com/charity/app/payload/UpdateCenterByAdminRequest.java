package com.charity.app.payload;

import jakarta.validation.constraints.Size;

public class UpdateCenterByAdminRequest {

    private String centerName;

    @Size(max = 1000)
    private String description;

    private String contactPhone;
    private String address;
    private String cardNumber;
    private String sheba;
    private String fullName;

    private Long provinceId;
    private Long cityId;

    private java.util.List<Long> categoryIds;

    public String getCenterName() { return centerName; }
    public void setCenterName(String centerName) { this.centerName = centerName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getSheba() { return sheba; }
    public void setSheba(String sheba) { this.sheba = sheba; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Long getProvinceId() { return provinceId; }
    public void setProvinceId(Long provinceId) { this.provinceId = provinceId; }
    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public java.util.List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(java.util.List<Long> categoryIds) { this.categoryIds = categoryIds; }
}
