package ru.tsvetikov.warehouse.router.model.dto.response;

import ru.tsvetikov.warehouse.router.model.enums.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        Role role,
        Double lastKnownX,
        Double lastKnownY,
        boolean active,
        LocalDateTime createdAt
) {}
