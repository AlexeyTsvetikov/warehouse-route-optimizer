package ru.tsvetikov.warehouse.router.model.dto.request;

public record OrderDetailRequest(
        String orderNumber,
        Integer quantity,
        String trackingNumber
) {}
