package ru.tsvetikov.warehouse.router.model.dto.response;

import ru.tsvetikov.warehouse.router.model.enums.RackType;

public record StorageRackResponse(
        Long id,
        String zone,
        RackType rackType,
        String description
) {}