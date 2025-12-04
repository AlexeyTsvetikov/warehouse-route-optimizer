package ru.tsvetikov.warehouse.router.model.dto.response;

import ru.tsvetikov.warehouse.router.model.enums.CellType;

public record RackCellSimpleResponse(
        Long id,
        String cellCode,
        CellType cellType,
        Double coordX,
        Double coordY,
        boolean occupied
) {}