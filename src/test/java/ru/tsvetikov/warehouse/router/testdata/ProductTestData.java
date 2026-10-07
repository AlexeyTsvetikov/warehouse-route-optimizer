package ru.tsvetikov.warehouse.router.testdata;

import ru.tsvetikov.warehouse.router.model.db.entity.Category;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.dto.request.ProductRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.ProductResponse;

public final class ProductTestData {

    private ProductTestData() {
    }

    public static ProductRequest request() {
        return new ProductRequest("brp-eng-001", "Поршень с шатуном", "Оригинальный поршень в сборе",
                0.85, 7.2, 7.2, 5.0, "Двигатель и топливная система");
    }

    public static ProductRequest request(String sku) {
        return new ProductRequest(sku, "Поршень с шатуном", "Оригинальный поршень в сборе",
                0.85, 7.2, 7.2, 5.0, "Двигатель и топливная система");
    }

    public static Product.ProductBuilder entityBuilder() {
        return Product.builder()
                .sku("BRP-ENG-001")
                .name("Поршень с шатуном")
                .description("Оригинальный поршень в сборе")
                .width(7.2).height(7.2).depth(5.0).weight(0.85)
                .category(category());
    }

    public static ProductResponse response() {
        return new ProductResponse(1L, "BRP-ENG-001", "Поршень с шатуном", "Оригинальный поршень в сборе",
                0.85, 7.2, 7.2, 5.0, 259.2, "Двигатель и топливная система");
    }

    private static Category category() {
        Category c = new Category();
        c.setName("Двигатель и топливная система");
        c.setDescription("Поршневые, цилиндры, карбюраторы, форсунки, топливные насосы");
        return c;
    }

}

