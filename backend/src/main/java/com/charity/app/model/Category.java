package com.charity.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A category of need (درمان، مسکن، تحصیل …).
 *
 * <p>Categories are attached to centres: a centre may only publish requests in the categories an
 * admin has granted it. A request itself carries exactly one category.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Latin URL slug, e.g. {@code darman}. Latin rather than Persian because this one is
     * admin-editable and appears in filter query strings, where a short stable token is easier to
     * reason about than a percent-encoded Persian word.
     */
    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    /** Background colour of the category chip, as {@code #RRGGBB}. Chosen from eight swatches. */
    @Column(name = "label_bg", length = 9)
    private String labelBg;

    /** Foreground colour of the category chip, as {@code #RRGGBB}. */
    @Column(name = "label_text", length = 9)
    private String labelText;

    /** Controls the order of the category grid and of chips; ties break on name. */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "icon_url")
    private String iconUrl;

    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
