package ru.tsvetikov.warehouse.router.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;

@Builder
public record WarehouseTaskRequest(
        @NotNull
        WarehouseTaskType type,
        @NotBlank
        String productSku,
        @NotNull @Positive
        Integer plannedQuantity,
        String sourceLocationCode,
        String targetLocationCode,
        String assignedUsername,
        String orderNumber,
        @PositiveOrZero Integer confirmedQuantity
) {}