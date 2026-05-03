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
import ru.tsvetikov.warehouse.router.model.dto.request.OrderRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderWithItemsRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderResponse;
import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;
import ru.tsvetikov.warehouse.router.service.OrderService;

import java.util.List;

@Tag(name = "Orders", description = "Управление заказами")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @Operation(summary = "Создать пустой заказ")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createEmpty(@RequestBody @Valid OrderRequest request) {
        return orderService.createEmptyOrder(request);
    }

    @Operation(summary = "Создать заказ с товарами")
    @PostMapping("/with-items")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createWithItems(@RequestBody @Valid OrderWithItemsRequest request) {
        return orderService.createOrderWithItems(request);
    }

    @Operation(summary = "Получить заказ по номеру")
    @GetMapping("/{orderNumber}")
    public OrderResponse getByNumber(@PathVariable String orderNumber) {
        return orderService.getByNumber(orderNumber);
    }

    @Operation(summary = "Получить все заказы с фильтрацией по статусам")
    @GetMapping
    public Page<OrderResponse> getAll(
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer perPage,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction order) {

        if (search != null && !search.isBlank()) {
            return orderService.search(search, page, perPage, sort, order);
        }
        if (statuses != null && !statuses.isEmpty()) {
            return orderService.getByStatuses(statuses, page, perPage, sort, order);
        }
        return orderService.getAll(page, perPage, sort, order);
    }

    @GetMapping("/search")
    public List<OrderResponse> search(@RequestParam String query) {
        return orderService.search(query, 1, 20, "orderNumber", Sort.Direction.ASC).getContent();
    }

    @Operation(summary = "Обновить заказ")
    @PutMapping("/{orderNumber}")
    public OrderResponse update(@PathVariable String orderNumber,
                                @RequestBody @Valid OrderRequest request) {
        return orderService.update(orderNumber, request);
    }

    @Operation(summary = "Удалить заказ")
    @DeleteMapping("/{orderNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String orderNumber) {
        orderService.delete(orderNumber);
    }

    @Operation(summary = "Начать обработку заказа")
    @PatchMapping("/{orderNumber}/start")
    public OrderResponse startProcessing(@PathVariable String orderNumber) {
        return orderService.startProcessing(orderNumber);
    }

    @Operation(summary = "Завершить заказ")
    @PatchMapping("/{orderNumber}/complete")
    public OrderResponse complete(@PathVariable String orderNumber) {
        return orderService.completeOrder(orderNumber);
    }

    @Operation(summary = "Завершить заказ приёмки")
    @PostMapping("/{orderNumber}/complete-inbound")
    public OrderResponse completeInboundOrder(@PathVariable String orderNumber) {
        return orderService.completeInboundOrder(orderNumber);
    }

    @Operation(summary = "Отменить заказ")
    @PatchMapping("/{orderNumber}/cancel")
    public OrderResponse cancel(@PathVariable String orderNumber) {
        return orderService.cancelOrder(orderNumber);
    }
}
