package ru.tsvetikov.warehouse.router.model.dto.request;

public record ChangePasswordRequest(
        String oldPassword,
        String newPassword
) {}
