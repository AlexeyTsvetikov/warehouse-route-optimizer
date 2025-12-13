//package ru.tsvetikov.warehouse.router.controllers;
//
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.*;
//import ru.tsvetikov.warehouse.router.model.dto.request.OrderItemRequest;
//import ru.tsvetikov.warehouse.router.model.dto.response.OrderItemResponse;
//import ru.tsvetikov.warehouse.router.service.OrderItemService;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/order-details")
//@RequiredArgsConstructor
//public class OrderItemController {
//    private final OrderItemService orderItemService;
//
//    @PostMapping
//    @ResponseStatus(HttpStatus.CREATED)
//    public OrderItemResponse create(@Valid @RequestBody OrderItemRequest request) {
//        return orderItemService.create(request);
//    }
//
//    @GetMapping("/{id}")
//    public OrderItemResponse getById(@PathVariable Long id) {
//        return orderItemService.getById(id);
//    }
//
//    @GetMapping
//    public List<OrderItemResponse> getAll() {
//        return orderItemService.getAll();
//    }
//
//    @GetMapping("/order/{orderId}")
//    public List<OrderItemResponse> getByOrderId(@PathVariable Long orderId) {
//        return orderItemService.getByOrderId(orderId);
//    }
//
//    @PutMapping("/{id}")
//    public OrderItemResponse update(@PathVariable Long id,
//                                    @Valid @RequestBody OrderItemRequest orderItemRequest) {
//        return orderItemService.update(id, orderItemRequest);
//    }
//
//    @DeleteMapping("/{id}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void delete(@PathVariable Long id) {
//        orderItemService.delete(id);
//    }
//}
