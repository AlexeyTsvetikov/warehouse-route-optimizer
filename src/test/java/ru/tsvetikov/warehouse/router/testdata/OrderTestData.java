package ru.tsvetikov.warehouse.router.testdata;

import ru.tsvetikov.warehouse.router.model.db.entity.Order;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderItemRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderWithItemsRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderResponse;
import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;
import ru.tsvetikov.warehouse.router.model.enums.OrderType;

import java.time.Instant;
import java.util.List;

public final class OrderTestData {

    private OrderTestData() {
    }

    public static OrderRequest request() {
        return new OrderRequest(OrderType.INBOUND, "CustomerName", "MSK", 2,
                Instant.parse("2026-01-15T10:00:00Z"));
    }

    public static OrderWithItemsRequest requestWithItems() {
        return new OrderWithItemsRequest(request(), List.of(OrderItemTestData.request()));
    }

    public static OrderWithItemsRequest requestWithItems(List<OrderItemRequest> items) {
        return new OrderWithItemsRequest(request(), items);
    }

    public static OrderWithItemsRequest requestWithoutItems() {
        return new OrderWithItemsRequest(request(), null);
    }

    public static Order.OrderBuilder entityBuilder() {
        return Order.builder()
                .type(OrderType.INBOUND)
                .customerName("CustomerName")
                .orderNumber("ORD-00001")
                .destinationRegion("MSK")
                .priority(2)
                .plannedDeparture(Instant.parse("2026-01-15T10:00:00Z"))
                .createdAt(Instant.parse("2026-01-15T09:00:00Z"));
    }

    public static OrderResponse response() {
        return new OrderResponse(1L, "ORD-00001", OrderType.INBOUND, "CustomerName",
                "MSK", 2, Instant.parse("2026-01-15T10:00:00Z"), OrderStatus.NEW,
                Instant.parse("2026-01-15T09:00:00Z"), null);
    }

    public static OrderResponse response(OrderStatus status) {
        return new OrderResponse(1L, "ORD-00001", OrderType.INBOUND, "CustomerName",
                "MSK", 2, Instant.parse("2026-01-15T10:00:00Z"), status,
                Instant.parse("2026-01-15T09:00:00Z"), null);
    }
}
