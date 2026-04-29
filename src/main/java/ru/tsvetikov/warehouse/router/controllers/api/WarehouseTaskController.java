package ru.tsvetikov.warehouse.router.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.WarehouseTaskResponse;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskStatus;
import ru.tsvetikov.warehouse.router.service.WarehouseTaskService;

import java.util.List;

@Tag(name = "Warehouse Tasks", description = "Управление заданиями операторам")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class WarehouseTaskController {
    private final WarehouseTaskService warehouseTaskService;

    @Operation(summary = "Создать новое задание")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseTaskResponse create(@RequestBody @Valid WarehouseTaskRequest request) {
        return warehouseTaskService.create(request);
    }

    @Operation(summary = "Получить задание по ID")
    @GetMapping("/{id}")
    public WarehouseTaskResponse getById(@PathVariable Long id) {
        return warehouseTaskService.getById(id);
    }

    @Operation(summary = "Получить все задания")
    @GetMapping
    public Page<WarehouseTaskResponse> getAll(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer perPage,
                                              @RequestParam(defaultValue = "createdAt") String sort,
                                              @RequestParam(defaultValue = "DESC") Sort.Direction order) {
        return warehouseTaskService.getAll(page, perPage, sort, order);
    }

    @Operation(summary = "Обновить задание")
    @PutMapping("/{id}")
    public WarehouseTaskResponse update(@PathVariable Long id,
                                        @RequestBody @Valid WarehouseTaskRequest request) {
        return warehouseTaskService.update(id, request);
    }

    @Operation(summary = "Отменить задание")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        warehouseTaskService.delete(id);
    }

    @Operation(summary = "Назначить задание оператору")
    @PatchMapping("/{id}/assign")
    public WarehouseTaskResponse assign(@PathVariable Long id,
                                        @RequestParam @NotBlank String username) {
        return warehouseTaskService.assignTask(id, username);
    }

    @Operation(summary = "Начать выполнение задания")
    @PatchMapping("/{id}/start")
    public WarehouseTaskResponse start(@PathVariable Long id) {
        return warehouseTaskService.startTask(id);
    }

    @Operation(summary = "Завершить задание")
    @PatchMapping("/{id}/complete")
    public WarehouseTaskResponse complete(@PathVariable Long id,
                                          @RequestParam(required = false) @PositiveOrZero Integer confirmedQuantity) {
        return warehouseTaskService.completeTask(id, confirmedQuantity);
    }

    @Operation(summary = "Получить задания пользователя по статусам")
    @GetMapping("/user/{username}")
    public Page<WarehouseTaskResponse> getByUser(
            @PathVariable String username,
            @RequestParam(required = false) List<WarehouseTaskStatus> statuses,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer perPage,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction order) {
        return warehouseTaskService.getTasksByUserAndStatus(username, statuses, page, perPage, sort, order);
    }
}