package ru.tsvetikov.warehouse.router.model.dto.request;

import ru.tsvetikov.warehouse.router.model.enums.Role;

public record UserRequest(
        String username,
        String password,
        Role role,
        Double lastKnownX,
        Double lastKnownY
) {}
