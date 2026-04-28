package ru.tsvetikov.warehouse.router.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tsvetikov.warehouse.router.model.dto.request.ProductRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.ProductResponse;
import ru.tsvetikov.warehouse.router.service.ProductService;

import java.util.List;


@Tag(name = "Products", description = "Управление товарами на складе")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @Operation(summary = "Создать новый товар")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@RequestBody @Valid ProductRequest request) {
        return productService.create(request);
    }

    @Operation(summary = "Получить товар по ID")
    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @Operation(summary = "Получить все товары с пагинацией")
    @GetMapping
    public Page<ProductResponse> getAll(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                        @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer perPage,
                                        @RequestParam(defaultValue = "sku") String sort,
                                        @RequestParam(defaultValue = "ASC") Sort.Direction order) {
        return productService.getAll(page, perPage, sort, order);
    }

    @Operation(summary = "Поиск товаров по SKU или названию")
    @GetMapping("/search")
    public List<ProductResponse> search(@RequestParam String query,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return productService.search(query, page, size, "sku", Sort.Direction.ASC).getContent();
    }

    @Operation(summary = "Обновить товар")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @RequestBody @Valid ProductRequest request) {
        return productService.update(id, request);
    }

    @Operation(summary = "Удалить товар")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    @Operation(summary = "Восстановить товар")
    @PatchMapping("/{id}/activate")
    public ProductResponse activate(@PathVariable Long id) {
        return productService.activate(id);
    }
}