package ru.tsvetikov.warehouse.router.model.dto.response;

import ru.tsvetikov.warehouse.router.model.enums.Role;


public record UserResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        Role role,
        Double lastKnownX,
        Double lastKnownY
) {}
