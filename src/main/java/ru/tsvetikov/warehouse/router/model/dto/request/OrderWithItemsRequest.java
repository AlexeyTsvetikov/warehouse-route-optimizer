package ru.tsvetikov.warehouse.router.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderWithItemsRequest(
        @Valid @NotNull OrderRequest order,
        @Valid List<@Valid OrderItemRequest> items
) {}
