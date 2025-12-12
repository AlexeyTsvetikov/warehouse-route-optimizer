package ru.tsvetikov.warehouse.router.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderDetailRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderDetailResponse;
import ru.tsvetikov.warehouse.router.service.OrderDetailService;

import java.util.List;

@RestController
@RequestMapping("/api/order-details")
@RequiredArgsConstructor
public class OrderDetailController {
    private final OrderDetailService orderDetailService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailResponse create(@Valid @RequestBody OrderDetailRequest request) {
        return orderDetailService.create(request);
    }

    @GetMapping("/{id}")
    public OrderDetailResponse getById(@PathVariable Long id) {
        return orderDetailService.getById(id);
    }

    @GetMapping
    public List<OrderDetailResponse> getAll() {
        return orderDetailService.getAll();
    }

    @GetMapping("/order/{orderId}")
    public List<OrderDetailResponse> getByOrderId(@PathVariable Long orderId) {
        return orderDetailService.getByOrderId(orderId);
    }

    @PutMapping("/{id}")
    public OrderDetailResponse update(@PathVariable Long id,
                                      @Valid @RequestBody OrderDetailRequest orderDetailRequest) {
        return orderDetailService.update(id, orderDetailRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        orderDetailService.delete(id);
    }
}
