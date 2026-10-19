package ru.tsvetikov.warehouse.router.testdata;

import ru.tsvetikov.warehouse.router.model.db.entity.Stock;
import ru.tsvetikov.warehouse.router.model.dto.response.StockResponse;

import java.time.Instant;

public final class StockTestData {

    private static final Instant INBOUND_DATE = Instant.parse("2026-01-15T09:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-15T10:00:00Z");

    private StockTestData() {
    }

    public static Stock.StockBuilder entityBuilder() {
        return Stock.builder()
                .product(ProductTestData.entityBuilder().id(1L).sku("BRP-ENG-001").build())
                .location(LocationTestData.entityBuilder().id(1L).code("PICK-01").build())
                .quantity(100)
                .reservedQuantity(0)
                .inboundDate(INBOUND_DATE)
                .updatedAt(UPDATED_AT);
    }

    public static StockResponse response() {
        return new StockResponse(1L, "BRP-ENG-001", "PICK-01", 100, 0,
                100, INBOUND_DATE, UPDATED_AT);
    }

    public static StockResponse response(Integer quantity, Integer reservedQuantity) {
        return new StockResponse(1L, "BRP-ENG-001", "PICK-01", quantity, reservedQuantity,
                quantity - reservedQuantity, INBOUND_DATE, UPDATED_AT);
    }
}
