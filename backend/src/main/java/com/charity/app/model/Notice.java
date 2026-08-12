package com.charity.app.model;

import com.charity.app.model.enums.NoticePlacement;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A site-wide announcement, shown either as the dismissible banner above the header or as a single
 * line in the footer.
 */
@Entity
@Table(name = "notices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    /** Full text. The top banner shows only the first line. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private NoticePlacement placement = NoticePlacement.FOOTER;

    /** Start of the display window. Null means "from now". */
    @Column(name = "start_at")
    private LocalDateTime startAt;

    /** End of the display window. Null means "indefinitely". */
    @Column(name = "end_at")
    private LocalDateTime endAt;

    /** Optional target for the «مشاهده» button. */
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Whether this is currently servable. Derived rather than stored, so an announcement lapses on
     * its own without a scheduled job.
     */
    public boolean isServableAt(LocalDateTime now) {
        return active
                && (startAt == null || !startAt.isAfter(now))
                && (endAt == null || !endAt.isBefore(now));
    }

    /** Drives the «منقضی» state in the admin table. */
    public boolean isExpiredAt(LocalDateTime now) {
        return endAt != null && endAt.isBefore(now);
    }
}
