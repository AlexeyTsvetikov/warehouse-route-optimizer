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
import ru.tsvetikov.warehouse.router.model.dto.request.CategoryRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.CategoryResponse;
import ru.tsvetikov.warehouse.router.service.CategoryService;

@Tag(name = "Categories", description = "Управление категориями товаров")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(summary = "Создать категорию")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@RequestBody @Valid CategoryRequest request) {
        return categoryService.create(request);
    }

    @Operation(summary = "Получить категорию по ID")
    @GetMapping("/{id}")
    public CategoryResponse getCategory(@PathVariable Long id) {
        return categoryService.getById(id);
    }

    @Operation(summary = "Получить все категории с пагинацией")
    @GetMapping
    public Page<CategoryResponse> getAllCategories(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                                   @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer perPage,
                                                   @RequestParam(defaultValue = "name") String sort,
                                                   @RequestParam(defaultValue = "ASC") Sort.Direction order) {
        return categoryService.getAll(page, perPage, sort, order);
    }

    @Operation(summary = "Поиск категорий по названию или описанию")
    @GetMapping("/search")
    public Page<CategoryResponse> searchCategories(@RequestParam String query,
                                                   @RequestParam(defaultValue = "1") @Min(1) Integer page,
                                                   @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer perPage,
                                                   @RequestParam(defaultValue = "name") String sort,
                                                   @RequestParam(defaultValue = "ASC") Sort.Direction order) {
        return categoryService.search(query, page, perPage, sort, order);
    }

    @Operation(summary = "Обновить категорию")
    @PutMapping("/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @RequestBody @Valid CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @Operation(summary = "Удалить категорию")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
    }

    @Operation(summary = "Восстановить категорию")
    @PatchMapping("/{id}/activate")
    public CategoryResponse activate(@PathVariable Long id) {
        return categoryService.activate(id);
    }
}

