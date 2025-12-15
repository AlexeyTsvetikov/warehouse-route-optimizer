package ru.tsvetikov.warehouse.router.model.db.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Сколько заказал клиент
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Сколько мы уже фактически собрали
    @Column(name = "collected_quantity", nullable = false)
    private Integer collectedQuantity = 0;

    // Вспомогательный метод для проверки
    public boolean isFullyCollected() {
        return collectedQuantity >= quantity;
    }
}
