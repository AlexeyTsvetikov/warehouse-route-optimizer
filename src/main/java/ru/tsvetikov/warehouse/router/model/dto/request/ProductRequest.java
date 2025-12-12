package ru.tsvetikov.warehouse.router.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.tsvetikov.warehouse.router.model.enums.Priority;

public record ProductRequest(
        @NotBlank
        String trackingNumber,
        @NotBlank @Size(max=100)
        String destinationRegion,
        Double width,
        Double height,
        Double depth,
        Double weight,
        Priority priority
) {
}
