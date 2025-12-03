package ru.tsvetikov.warehouse.router.model.dto.response;

import ru.tsvetikov.warehouse.router.model.enums.Priority;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String trackingNumber,
        String destinationRegion,
        Double width,
        Double height,
        Double depth,
        Double weight,
        Double volume,
        Priority priority,
        LocalDateTime createdAt
) {}
