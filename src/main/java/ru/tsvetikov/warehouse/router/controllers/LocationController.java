package ru.tsvetikov.warehouse.router.controllers;

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
import ru.tsvetikov.warehouse.router.model.dto.request.LocationRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.LocationResponse;
import ru.tsvetikov.warehouse.router.service.LocationService;

@Tag(name = "Locations", description = "Управление ячейками склада")
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;

    @Operation(summary = "Создать новую ячейку")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse create(@RequestBody @Valid LocationRequest request) {
        return locationService.create(request);
    }

    @Operation(summary = "Получить ячейку по ID")
    @GetMapping("/{id}")
    public LocationResponse getById(@PathVariable Long id) {
        return locationService.getById(id);
    }

    @Operation(summary = "Получить все ячейки с пагинацией")
    @GetMapping
    public Page<LocationResponse> getAll(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                         @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer perPage,
                                         @RequestParam(defaultValue = "code") String sort,
                                         @RequestParam(defaultValue = "ASC") Sort.Direction order) {
        return locationService.getAll(page, perPage, sort, order);
    }

    @Operation(summary = "Обновить ячейку")
    @PutMapping("/{id}")
    public LocationResponse update(@PathVariable Long id, @RequestBody @Valid LocationRequest request) {
        return locationService.update(id, request);
    }

    @Operation(summary = "Удалить ячейку")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        locationService.delete(id);
    }

    @Operation(summary = "Восстановить ячейку")
    @PatchMapping("/{id}/activate")
    public LocationResponse activate(@PathVariable Long id) {
        return locationService.activate(id);
    }
}