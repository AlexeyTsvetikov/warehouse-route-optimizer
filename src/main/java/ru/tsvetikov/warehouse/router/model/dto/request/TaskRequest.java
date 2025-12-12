package ru.tsvetikov.warehouse.router.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.tsvetikov.warehouse.router.model.enums.TaskType;

public record TaskRequest(
        @NotNull
        TaskType type,
        String username,
        @NotBlank
        String productTrackingNumber,
        @NotBlank
        String sourceCellCode,
        @NotBlank
        String targetCellCode,
        String orderNumber
) {}
