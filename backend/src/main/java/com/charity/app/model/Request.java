package com.charity.app.model;

import com.charity.app.common.JsonListConverter;
import com.charity.app.common.JsonMapConverter;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.Urgency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A published request for help. Formerly {@code CharityCase}.
 *
 * <p>The platform only announces requests -- there is no online payment and no donation tracking.
 * {@code amountNeeded} is shown publicly so a visitor knows the scale of the need before contacting
 * the centre; there is deliberately no "amount collected" counterpart.
 */
@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short human-readable identifier shown to visitors and quotable over the phone, e.g. RQ-1024. */
    @Column(nullable = false, length = 32, unique = true)
    private String code;

    /** Persian URL slug, always suffixed with {@link #code} so it cannot collide. */
    @Column(nullable = false, unique = true)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Where the help is needed. Nullable in the database because it is backfilled from the owning
     * centre, whose own city is optional; new requests require it at the DTO level instead.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @Column(nullable = false)
    private String title;

    @Column(length = 3000)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountNeeded;

    /** When the need stops being useful to meet, e.g. a surgery date. */
    private LocalDate deadline;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "contact_info", length = 500)
    private String contactInfo;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "details_json", columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, Object> details = new HashMap<>();

    @Convert(converter = JsonListConverter.class)
    @Column(name = "documents_json", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> documents = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    /** Why an admin rejected or deactivated this. Mandatory when rejecting. */
    @Column(name = "status_note", length = 1000)
    private String statusNote;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Urgency urgency = Urgency.MEDIUM;

    /**
     * Mirrors {@link Urgency#rank()} so ordering is a column sort instead of a {@code CASE WHEN}
     * repeated in every query. Kept in sync by {@link #setUrgency}; never set it directly.
     */
    @Column(name = "urgency_rank", nullable = false)
    @Builder.Default
    private int urgencyRank = Urgency.MEDIUM.rank();

    /** First time this went live. Used for {@code datePublished} and to freeze the slug. */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * Soft delete. The row survives so the URL can answer 410 Gone rather than 404 -- search engines
     * drop 410s far faster, and once the row is gone there is no way to tell "removed" from
     * "never existed".
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Optional admin overrides for the search-result title and snippet. */
    @Column(name = "meta_title", length = 70)
    private String metaTitle;

    @Column(name = "meta_description", length = 160)
    private String metaDescription;

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

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency == null ? Urgency.MEDIUM : urgency;
        this.urgencyRank = this.urgency.rank();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** A slug may only change while the request has never been public. */
    public boolean isSlugFrozen() {
        return publishedAt != null;
    }
}
