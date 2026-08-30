package com.charity.app.model;

import com.charity.app.common.Paging;
import com.charity.app.model.enums.CenterStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A charity centre. Created only by an admin -- there is no public registration.
 */
@Entity
@Table(name = "centers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Center {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String name;

    /** Persian URL slug for {@code /centers/<slug>}. */
    @Column(nullable = false, unique = true)
    private String slug;

    /**
     * The categories this centre is allowed to publish in. A request's category is validated
     * against this set on every write.
     *
     * <p>Batched rather than fetch-joined. A collection in an {@code @EntityGraph} multiplies the
     * result rows, which costs the paged listings their SQL {@code LIMIT} -- see the note in
     * {@code CenterRepository}. {@link Paging#MAX_SIZE} is the batch size because it is the largest
     * page the API will serve, so a full page of centres loads its categories in one statement.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "center_categories",
            joinColumns = @JoinColumn(name = "center_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @BatchSize(size = Paging.MAX_SIZE)
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    @Column(name = "full_name")
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id")
    private Province province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @Column(length = 1000)
    private String description;

    @Column(name = "contact_phone")
    private String contactPhone;

    /** Free text, e.g. «شنبه تا چهارشنبه ۹ تا ۱۷». Shown on the request detail sidebar. */
    @Column(name = "response_hours", length = 120)
    private String responseHours;

    @Column(name = "address", length = 1000)
    private String address;

    @Column(name = "card_number")
    private String cardNumber;

    @Column(name = "sheba")
    private String sheba;

    @Column(name = "logo_url")
    private String logoUrl;

    /**
     * The centre's own paperwork -- licence, articles, accounts -- rendered on its public page.
     *
     * <p>Cascades on delete so removing a centre cannot fail on the {@code center_documents}
     * foreign key; the files behind the rows are unlinked separately in {@code CenterService},
     * because the database knows nothing about the disk.
     *
     * <p>Batched for the same reason as {@link #categories}, and for one of its own: the admin
     * centres listing renders documents for every row, which was a query per centre.
     */
    @OneToMany(mappedBy = "center", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @BatchSize(size = Paging.MAX_SIZE)
    @Builder.Default
    private List<CenterDocument> documents = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CenterStatus status = CenterStatus.APPROVED;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return status == CenterStatus.APPROVED;
    }
}
