package ru.tsvetikov.warehouse.router.model.dto.request;

import java.time.LocalDateTime;

public record OrderRequest(
        String orderNumber,
        String destinationRegion,
        LocalDateTime plannedDeparture
) {}
