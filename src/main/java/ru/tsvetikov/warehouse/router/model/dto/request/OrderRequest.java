package ru.tsvetikov.warehouse.router.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record OrderRequest(
        @NotBlank
        String orderNumber,
        @NotBlank
        String destinationRegion,
        @NotNull
        LocalDateTime plannedDeparture
) {}
