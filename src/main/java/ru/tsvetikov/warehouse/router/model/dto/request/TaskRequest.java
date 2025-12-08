package ru.tsvetikov.warehouse.router.model.dto.request;

import ru.tsvetikov.warehouse.router.model.enums.TaskType;

public record TaskRequest(
        TaskType type,
        String username,
        String productTrackingNumber,
        String sourceCellCode,
        String targetCellCode,
        String orderNumber
) {}
