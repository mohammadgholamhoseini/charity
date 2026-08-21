package com.charity.app.model;

import com.charity.app.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One uploaded document belonging to a centre -- its licence, articles, accounts.
 *
 * <p>The centre's own paperwork rather than a beneficiary's, and it renders on the centre's public
 * page. Column-identical to {@link RequestDocument} but a separate table: the owner foreign key is
 * different, and a shared table with two nullable owner columns would let a row belong to both or
 * to neither.
 */
@Entity
@Table(name = "center_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CenterDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private DocumentCategory category;

    /** Generated UUID name on disk. Never shown; never taken from the uploader. */
    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "original_filename")
    private String originalFilename;

    private String title;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    /** Either role: a centre uploads its own, an admin uploads on the create-centre form. */
    @Enumerated(EnumType.STRING)
    @Column(name = "uploaded_by_role", length = 16)
    private UserRole uploadedByRole;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
}
