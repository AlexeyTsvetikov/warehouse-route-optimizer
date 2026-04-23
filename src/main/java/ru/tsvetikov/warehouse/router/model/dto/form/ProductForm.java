package ru.tsvetikov.warehouse.router.model.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductForm {
    @NotBlank(message = "Артикул обязателен")
    @Size(max = 100)
    private String sku;

    @NotBlank(message = "Наименование обязательно")
    private String name;

    @Size(max = 1000)
    private String description;

    @Positive
    private Double weight;

    @Positive
    private Double width;

    @Positive
    private Double height;

    @Positive
    private Double depth;

    @NotBlank(message = "Название категории обязательно")
    private String categoryName;
}
