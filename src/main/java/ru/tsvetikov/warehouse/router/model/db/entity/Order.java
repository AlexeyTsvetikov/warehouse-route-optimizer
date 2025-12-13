package ru.tsvetikov.warehouse.router.model.db.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;
import ru.tsvetikov.warehouse.router.model.enums.OrderType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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

    // Номер заказа из внешней системы "ORD-2023-001"
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private OrderType type;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    // Регион доставки. Может использоваться для группировки заказов
    @Column(name = "destination_region", nullable = false, length = 100)
    private String destinationRegion;

    // Приоритет: 3 - низкий, 2 - средний, 1 - высокий
    @Column(name = "priority", nullable = false)
    private Integer priority = 2;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OrderStatus status = OrderStatus.NEW;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // Когда заказ должен уехать
    @Column(name = "planned_departure", nullable = false)
    private Instant plannedDeparture;

    // Когда заказ реально собрали (для статистики)
    @Column(name = "completed_at")
    private Instant completedAt;

    // Список товаров в заказе
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    // Задачи на сборку, связанные с этим заказом
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private Set<WarehouseTask> warehouseTasks = new LinkedHashSet<>();
}
