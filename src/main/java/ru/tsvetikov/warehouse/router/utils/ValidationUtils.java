package ru.tsvetikov.warehouse.router.utils;

import org.springframework.validation.BindingResult;

import java.util.stream.Collectors;

public class ValidationUtils {

    public static String getValidationErrors(BindingResult result) {
        return result.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }
}
