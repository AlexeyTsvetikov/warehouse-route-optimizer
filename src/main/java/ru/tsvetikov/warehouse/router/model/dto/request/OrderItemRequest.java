package ru.tsvetikov.warehouse.router.model.dto.request;

import jakarta.validation.constraints.*;

public record OrderItemRequest(
        @NotBlank String productSku,
        @NotNull @Positive Integer quantity
) {}