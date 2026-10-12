package ru.tsvetikov.warehouse.router.testdata;

import ru.tsvetikov.warehouse.router.model.db.entity.OrderItem;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderItemRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderItemResponse;

public final class OrderItemTestData {

    private OrderItemTestData() {
    }

    public static OrderItemRequest request() {
        return new OrderItemRequest("BRP-ENG-001", 10);
    }

    public static OrderItemRequest request(String productSku, Integer quantity) {
        return new OrderItemRequest(productSku, quantity);
    }

    public static OrderItem.OrderItemBuilder entityBuilder() {
        return OrderItem.builder()
                .order(OrderTestData.entityBuilder().id(1L).build())
                .product(ProductTestData.entityBuilder().id(1L).build())
                .quantity(10)
                .collectedQuantity(10);
    }

    public static OrderItemResponse response() {
        return new OrderItemResponse(1L, "ORD-00001", "BRP-ENG-001",
                "Поршень с шатуном", 10, 10, true);
    }
}
