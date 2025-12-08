package ru.tsvetikov.warehouse.router.model.dto.response;

public record OrderDetailResponse(
        Long id,
        String orderNumber,
        Integer quantity,
        String trackingNumber
) {}
