package com.charity.app.model;

import com.charity.app.model.enums.DocumentScope;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A kind of uploaded document (مستندات مالی، مجوز فعالیت …), admin-managed.
 *
 * <p>Deliberately not the same thing as {@link Category}, which is the public taxonomy of *need*
 * that centres are granted and requests are filed under. This one only classifies paperwork.
 *
 * <p>{@link #scope} keeps the request list and the centre list in one table: they carry identical
 * columns, and uniqueness is per scope, so «صورت مالی» may exist on both sides without collision.
 */
@Entity
@Table(name = "document_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Which of the two lists this belongs to. Part of both unique keys. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DocumentScope scope;

    @Column(nullable = false)
    private String name;

    /** Latin slug, unique within the scope. Hand-written or derived from the name. */
    @Column(nullable = false, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    /** Orders the upload picker and the grouped public list; ties break on name. */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    /**
     * An inactive category disappears from every upload picker while documents already filed under
     * it keep rendering with its name. This is the preferred alternative to deleting one.
     */
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
