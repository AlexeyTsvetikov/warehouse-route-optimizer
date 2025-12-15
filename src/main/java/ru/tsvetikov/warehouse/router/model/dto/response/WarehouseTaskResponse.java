package ru.tsvetikov.warehouse.router.model.dto.response;

import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskStatus;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;

import java.time.LocalDateTime;

public record WarehouseTaskResponse(
        Long id,
        String taskNumber,
        WarehouseTaskType type,
        WarehouseTaskStatus status,
        String assignedUsername,
        String productSku,
        Integer plannedQuantity,
        Integer confirmedQuantity,
        String sourceLocationCode,
        String targetLocationCode,
        String orderNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {}