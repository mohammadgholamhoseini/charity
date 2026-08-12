package com.charity.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cities", uniqueConstraints = @UniqueConstraint(columnNames = {"province_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * LAZY now that cities are exposed as a flattened {@code CityRef} rather than as raw entities.
     * It had to be EAGER while public endpoints serialised the entity directly, which meant every
     * city in a dropdown dragged a nested province object along with it.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;
}
