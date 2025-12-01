package ru.tsvetikov.warehouse.router.model.db.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import ru.tsvetikov.warehouse.router.model.enums.Priority;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tracking_number", nullable = false, unique = true, length = 50)
    private String trackingNumber;

    @Column(name = "destination_region", nullable = false, length = 100)
    private String destinationRegion;

    @Column(name = "width")
    private Double width;

    @Column(name = "height")
    private Double height;

    @Column(name = "depth")
    private Double depth;

    @Column(name = "volume", insertable = false, updatable = false)
    private Double volume;

    @Column(name = "weight")
    private Double weight;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private Priority priority = Priority.NORMAL;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<OrderDetail> orderDetails = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<Task> tasks = new LinkedHashSet<>();

}
