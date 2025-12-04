package ru.tsvetikov.warehouse.router.model.dto.request;

import ru.tsvetikov.warehouse.router.model.enums.RackType;

public record StorageRackRequest(
        String zone,
        RackType rackType,
        String description
) {}
