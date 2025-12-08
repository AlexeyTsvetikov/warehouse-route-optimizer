package ru.tsvetikov.warehouse.router.model.dto.response;

import ru.tsvetikov.warehouse.router.model.enums.TaskStatus;
import ru.tsvetikov.warehouse.router.model.enums.TaskType;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String taskNumber,
        TaskType type,
        TaskStatus status,
        String username,
        String productTrackingNumber,
        String sourceCellCode,
        String targetCellCode,
        String orderNumber,
        LocalDateTime createdAt,
        LocalDateTime assignedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {}
