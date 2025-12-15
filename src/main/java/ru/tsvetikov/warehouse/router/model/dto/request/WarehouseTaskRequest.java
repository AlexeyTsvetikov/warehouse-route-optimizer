package ru.tsvetikov.warehouse.router.model.dto.request;

import jakarta.validation.constraints.*;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;

public record WarehouseTaskRequest(
        @NotNull
        WarehouseTaskType type,
        @NotBlank
        String productSku,
        @NotNull @Positive
        Integer plannedQuantity,
        @NotBlank
        String sourceLocationCode,
        @NotBlank
        String targetLocationCode,
        String assignedUsername,
        String orderNumber,
        @PositiveOrZero Integer confirmedQuantity
) {}