package ru.tsvetikov.warehouse.router.model.dto.request;

import ru.tsvetikov.warehouse.router.model.enums.CellType;

public record RackCellRequest(
        Long storageRackId,
        String cellCode,
        CellType cellType,
        Double coordX,
        Double coordY,
        Double maxVolume,
        Double currentVolume,
        Boolean occupied
) {}
