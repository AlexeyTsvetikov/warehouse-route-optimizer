package ru.tsvetikov.warehouse.router.model.dto.response;

import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;
import ru.tsvetikov.warehouse.router.model.enums.OrderType;

import java.time.Instant;

public record OrderResponse(
        Long id,
        String orderNumber,
        OrderType type,
        String customerName,
        String destinationRegion,
        Integer priority,
        Instant plannedDeparture,
        OrderStatus status,
        Instant createdAt,
        Instant completedAt
) {}