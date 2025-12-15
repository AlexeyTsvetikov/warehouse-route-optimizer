package ru.tsvetikov.warehouse.router.model.dto.request;

import jakarta.validation.constraints.*;
import ru.tsvetikov.warehouse.router.model.enums.OrderType;

import java.time.Instant;

public record OrderRequest(
        @NotBlank @Size(max = 50) String orderNumber,
        @NotNull OrderType type,
        @Size(max = 100) String customerName,
        @NotBlank @Size(max = 100) String destinationRegion,
        @Min(1) @Max(3) Integer priority,
        @NotNull @Future Instant plannedDeparture
) {}
