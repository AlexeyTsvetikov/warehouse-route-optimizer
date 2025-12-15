package ru.tsvetikov.warehouse.router.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderItemRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderItemResponse;
import ru.tsvetikov.warehouse.router.service.OrderItemService;

@Tag(name = "Order Items", description = "Управление позициями заказов")
@RestController
@RequestMapping("/api/orders/{orderNumber}/items")
@RequiredArgsConstructor
public class OrderItemController {
    private final OrderItemService orderItemService;

    @Operation(summary = "Добавить товар в заказ")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderItemResponse create(@PathVariable String orderNumber,
                                    @RequestBody @Valid OrderItemRequest request) {
        return orderItemService.create(orderNumber, request);
    }

    @Operation(summary = "Получить позицию по ID")
    @GetMapping("/{id}")
    public OrderItemResponse getById(@PathVariable String orderNumber, @PathVariable Long id) {
        return orderItemService.getById(orderNumber, id);
    }

    @Operation(summary = "Получить все позиции заказа с пагинацией")
    @GetMapping
    public Page<OrderItemResponse> getAll(@PathVariable String orderNumber,
                                          @RequestParam(defaultValue = "1") @Min(1) Integer page,
                                          @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer perPage,
                                          @RequestParam(defaultValue = "priority") String sort,
                                          @RequestParam(defaultValue = "ASC") Sort.Direction order) {
        return orderItemService.getByOrder(orderNumber, page, perPage, sort, order);
    }

    @Operation(summary = "Обновить позицию")
    @PutMapping("/{id}")
    public OrderItemResponse update(@PathVariable String orderNumber, @PathVariable Long id,
                                    @RequestBody @Valid OrderItemRequest request) {
        return orderItemService.update(orderNumber, id, request);
    }

    @Operation(summary = "Удалить позицию")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String orderNumber, @PathVariable Long id) {
        orderItemService.delete(orderNumber, id);
    }

    @Operation(summary = "Обновить собранное количество")
    @PatchMapping("/{id}/collected")
    public OrderItemResponse updateCollectedQuantity(@PathVariable String orderNumber, @PathVariable Long id,
                                                     @RequestParam @PositiveOrZero Integer quantity) {
        return orderItemService.updateCollectedQuantity(orderNumber, id, quantity);
    }
}