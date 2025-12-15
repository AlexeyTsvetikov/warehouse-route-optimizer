package ru.tsvetikov.warehouse.router.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;


public record ProductRequest(
        @NotBlank @Size(max = 100) String sku,
        @NotBlank String name,
        @Size(max = 1000) String description,
        @Positive Double weight,
        @Positive Double width,
        @Positive Double height,
        @Positive Double depth,
        @NotBlank String categoryName
) {}
