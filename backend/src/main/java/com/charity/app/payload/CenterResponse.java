package com.charity.app.payload;

import java.util.List;

public class CenterResponse {
    private Long id;
    private String name;
    private String fullName;
    private String description;
    private String contactPhone;
    private String address;
    private String cardNumber;
    private String sheba;
    private String logoUrl;
    private String status;
    private ProvinceInfo province;
    private CityInfo city;
    private List<CategoryInfo> categories;
    private String createdAt;

    public CenterResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
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
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public ProvinceInfo getProvince() { return province; }
    public void setProvince(ProvinceInfo province) { this.province = province; }
    public CityInfo getCity() { return city; }
    public void setCity(CityInfo city) { this.city = city; }
    public List<CategoryInfo> getCategories() { return categories; }
    public void setCategories(List<CategoryInfo> categories) { this.categories = categories; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public static class ProvinceInfo {
        private Long id;
        private String name;
        public ProvinceInfo() {}
        public ProvinceInfo(Long id, String name) { this.id = id; this.name = name; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class CityInfo {
        private Long id;
        private String name;
        public CityInfo() {}
        public CityInfo(Long id, String name) { this.id = id; this.name = name; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class CategoryInfo {
        private Long id;
        private String name;
        public CategoryInfo() {}
        public CategoryInfo(Long id, String name) { this.id = id; this.name = name; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
