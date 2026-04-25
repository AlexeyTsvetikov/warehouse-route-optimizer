package ru.tsvetikov.warehouse.router.event;

import ru.tsvetikov.warehouse.router.model.db.entity.OrderItem;

public record OrderItemCompletedEvent(OrderItem orderItem) {
}
