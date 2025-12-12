package ru.tsvetikov.warehouse.router.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.tsvetikov.warehouse.router.model.enums.RackType;

public record StorageRackRequest(
        @NotBlank
        String zone,
        @NotNull
        RackType rackType,
        String description
) {}
