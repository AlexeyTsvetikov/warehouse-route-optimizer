package ru.tsvetikov.warehouse.router.model.dto.request;

import ru.tsvetikov.warehouse.router.model.enums.Priority;

public record ProductRequest(
        String trackingNumber,
        String destinationRegion,
        Double width,
        Double height,
        Double depth,
        Double weight,
        Priority priority
) {}
