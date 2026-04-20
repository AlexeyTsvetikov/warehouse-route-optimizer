package ru.tsvetikov.warehouse.router.model.dto.form;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductForm {
    private String sku;
    private String name;
    private String description;
    private Double weight;
    private Double width;
    private Double height;
    private Double depth;
    private String categoryName;
}
