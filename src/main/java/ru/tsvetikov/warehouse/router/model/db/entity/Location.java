package ru.tsvetikov.warehouse.router.model.db.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;
import ru.tsvetikov.warehouse.router.model.enums.LocationType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Entity
@Table(name = "locations")
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // Штрихкод места, например "A-01-02" (Ряд А, Стеллаж 1, Полка 2)
    @Column(name = "code", unique = true, length = 100, nullable = false)
    private String code;

    @Column(name = "type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private LocationType type;

    @Column(name = "width")
    private Double width;

    @Column(name = "height")
    private Double height;

    @Column(name = "depth")
    private Double depth;

    @Column(name = "volume", insertable = false, updatable = false)
    private Double volume;

    @Column(name = "max_weight")
    private Double maxWeight;

    @Column(name = "x_coord", nullable = false)
    private Double xCoord;

    @Column(name = "y_coord", nullable = false)
    private Double yCoord;

    @Column(name = "description", length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "location", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "location-stock")
    private List<Stock> stocks = new ArrayList<>();

}
