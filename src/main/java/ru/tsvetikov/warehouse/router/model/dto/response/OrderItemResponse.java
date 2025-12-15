package ru.tsvetikov.warehouse.router.model.dto.response;

public record OrderItemResponse(
        Long id,
        String orderNumber,
        String productSku,
        String productName,
        Integer quantity,
        Integer collectedQuantity,
        Boolean isFullyCollected
) {}