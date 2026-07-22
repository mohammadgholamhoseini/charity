package com.charity.app.payload;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CharityCaseResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal amountNeeded;
    private BigDecimal amountCollected;
    private String imageUrl;
    private String contactInfo;
    private Map<String, Object> details;
    private List<String> documents;
    private String status;
    private String urgency;
    private Long centerId;
    private String centerName;
    private String centerCategory;
    private Long categoryId;
    private String categoryName;
    private String createdAt;

    public CharityCaseResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmountNeeded() { return amountNeeded; }
    public void setAmountNeeded(BigDecimal amountNeeded) { this.amountNeeded = amountNeeded; }
    public BigDecimal getAmountCollected() { return amountCollected; }
    public void setAmountCollected(BigDecimal amountCollected) { this.amountCollected = amountCollected; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    public List<String> getDocuments() { return documents; }
    public void setDocuments(List<String> documents) { this.documents = documents; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public Long getCenterId() { return centerId; }
    public void setCenterId(Long centerId) { this.centerId = centerId; }
    public String getCenterName() { return centerName; }
    public void setCenterName(String centerName) { this.centerName = centerName; }
    public String getCenterCategory() { return centerCategory; }
    public void setCenterCategory(String centerCategory) { this.centerCategory = centerCategory; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
