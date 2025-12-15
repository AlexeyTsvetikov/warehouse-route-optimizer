package ru.tsvetikov.warehouse.router.model.dto.response;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        Double weight,
        Double width,
        Double height,
        Double depth,
        Double volume,
        String categoryName
) {}
