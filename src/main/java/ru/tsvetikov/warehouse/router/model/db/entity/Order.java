package ru.tsvetikov.warehouse.router.model.db.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "destination_region", nullable = false, length = 100)
    private String destinationRegion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private OrderStatus status = OrderStatus.FORMING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "planned_departure", nullable = false)
    private LocalDateTime plannedDeparture;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private Set<OrderDetail> orderDetails = new LinkedHashSet<>();

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private Set<Task> tasks = new LinkedHashSet<>();

}
