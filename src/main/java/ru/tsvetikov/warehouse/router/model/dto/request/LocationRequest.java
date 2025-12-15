package ru.tsvetikov.warehouse.router.model.dto.request;

import jakarta.validation.constraints.*;
import ru.tsvetikov.warehouse.router.model.enums.LocationType;

public record LocationRequest(
        @NotBlank @Size(max = 100) String code,
        @NotNull LocationType type,
        @Positive Double width,
        @Positive Double height,
        @Positive Double depth,
        @Positive Double maxWeight,
        @Min(0) Double coordX,
        @Min(0) Double coordY,
        @Size(max = 500) String description
) {
}