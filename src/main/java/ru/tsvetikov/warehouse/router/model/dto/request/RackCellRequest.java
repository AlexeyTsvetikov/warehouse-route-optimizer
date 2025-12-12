package ru.tsvetikov.warehouse.router.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.tsvetikov.warehouse.router.model.enums.CellType;

public record RackCellRequest(
        @NotNull
        Long storageRackId,
        @NotBlank
        String cellCode,
        @NotNull
        CellType cellType,
        @NotNull @Positive
        Double coordX,
        @NotNull @Positive
        Double coordY,
        @NotNull @Positive
        Double maxVolume,
        Double currentVolume,
        @NotNull
        Boolean occupied
) {}
