package ru.tsvetikov.warehouse.router.model.dto.response;

import java.time.Instant;

public record StockResponse(
        Long id,
        String productSku,
        String locationCode,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        Instant inboundDate,
        Instant updatedAt
) {}
