package com.charity.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "charity_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharityCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(length = 3000)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountNeeded;

    @Builder.Default
    private BigDecimal amountCollected = BigDecimal.ZERO;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "contact_info", length = 500)
    private String contactInfo;

    @Column(columnDefinition = "TEXT")
    @JsonIgnore
    private String detailsJson;

    @Column(columnDefinition = "TEXT")
    @JsonIgnore
    private String documentsJson;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Urgency urgency = Urgency.MEDIUM;

    @Column(name = "telegram_posted")
    @Builder.Default
    private boolean telegramPosted = false;

    @Column(name = "telegram_message_id")
    private Integer telegramMessageId;

    @Column(name = "bale_posted")
    @Builder.Default
    private boolean balePosted = false;

    @Column(name = "bale_message_id")
    private Integer baleMessageId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Transient
    private Map<String, Object> details;

    @Transient
    private java.util.List<String> documents;

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    public Map<String, Object> getDetails() {
        if (details == null && detailsJson != null && !detailsJson.isBlank()) {
            try {
                details = MAPPER.readValue(detailsJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                details = java.util.Collections.emptyMap();
            }
        }
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
        try {
            this.detailsJson = details == null ? null : MAPPER.writeValueAsString(details);
        } catch (Exception e) {
            this.detailsJson = null;
        }
    }

    public java.util.List<String> getDocuments() {
        if (documents == null && documentsJson != null && !documentsJson.isBlank()) {
            try {
                documents = MAPPER.readValue(documentsJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
            } catch (Exception e) {
                documents = java.util.Collections.emptyList();
            }
        }
        return documents;
    }

    public void setDocuments(java.util.List<String> documents) {
        this.documents = documents;
        try {
            this.documentsJson = documents == null ? null : MAPPER.writeValueAsString(documents);
        } catch (Exception e) {
            this.documentsJson = null;
        }
    }

    public enum Status {
        PENDING, PUBLISHED, COMPLETED, REJECTED, INACTIVE
    }

    public enum Urgency {
        LOW, MEDIUM, HIGH, URGENT
    }
}
