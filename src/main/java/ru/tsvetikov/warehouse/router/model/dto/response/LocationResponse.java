package ru.tsvetikov.warehouse.router.model.dto.response;

import ru.tsvetikov.warehouse.router.model.enums.LocationType;

public record LocationResponse(
        Long id,
        String code,
        LocationType type,
        Double width,
        Double height,
        Double depth,
        Double volume,
        Double maxWeight,
        Double coordX,
        Double coordY,
        String description
) {}