package ru.tsvetikov.warehouse.router.model.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderItemForm {
    @NotBlank
    private String productSku;

    @Positive
    private Integer quantity;
}
