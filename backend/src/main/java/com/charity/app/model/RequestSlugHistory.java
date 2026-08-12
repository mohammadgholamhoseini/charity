package com.charity.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A slug a request used to have.
 *
 * <p>Exists so that changing a published request's slug answers 301 on the old URL instead of 404.
 * Without it, an admin fixing a typo in a title would silently de-index a page that already ranks
 * and is already linked from the Bale channel.
 */
@Entity
@Table(name = "request_slug_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestSlugHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @Column(name = "old_slug", nullable = false, unique = true)
    private String oldSlug;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
