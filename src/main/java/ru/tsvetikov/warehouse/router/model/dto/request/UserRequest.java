package ru.tsvetikov.warehouse.router.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.tsvetikov.warehouse.router.model.enums.Role;

public record UserRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 6) String password,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @NotNull Role role,
        @Min(0) Double lastKnownX,
        @Min(0) Double lastKnownY
) {}
