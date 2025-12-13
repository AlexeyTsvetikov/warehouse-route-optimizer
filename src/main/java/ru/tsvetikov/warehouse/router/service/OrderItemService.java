//package ru.tsvetikov.warehouse.router.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
//import ru.tsvetikov.warehouse.router.model.db.entity.Order;
//import ru.tsvetikov.warehouse.router.model.db.entity.OrderItem;
//import ru.tsvetikov.warehouse.router.model.db.entity.Product;
//import ru.tsvetikov.warehouse.router.model.db.repository.OrderItemRepository;
//import ru.tsvetikov.warehouse.router.model.dto.request.OrderItemRequest;
//import ru.tsvetikov.warehouse.router.model.dto.response.OrderItemResponse;
//import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;
//import ru.tsvetikov.warehouse.router.model.mapper.OrderItemMapper;
//import ru.tsvetikov.warehouse.router.service.utill.EntityFinder;
//
//import java.util.List;
//import java.util.stream.Collectors;

//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class OrderItemService {
//    private final OrderItemRepository orderItemRepository;
//    private final OrderItemMapper orderItemMapper;
//    private final EntityFinder entityFinder;
//
//    @Transactional
//    public OrderItemResponse create(OrderItemRequest request) {
//        validateRequest(request);
//
//        Order order = entityFinder.findOrderByNumber(request.orderNumber());
//        Product product = entityFinder.findProductByTrackingNumber(request.trackingNumber());
//
//        validateOrderForAddingProducts(order);
//        validateProductNotInActiveOrder(product);
//
//        OrderItem orderItem = new OrderItem();
//        orderItem.setQuantity(request.quantity());
//        orderItem.setOrder(order);
//        orderItem.setProduct(product);
//
//        OrderItem saved = orderItemRepository.save(orderItem);
//        return orderItemMapper.toResponseDto(saved);
//    }
//
//    @Transactional(readOnly = true)
//    public OrderItemResponse getById(Long id) {
//        OrderItem orderItem = getOrderDetailEntityById(id);
//        return orderItemMapper.toResponseDto(orderItem);
//    }
//
//    @Transactional(readOnly = true)
//    public List<OrderItemResponse> getAll() {
//        return orderItemRepository.findAll().stream()
//                .map(orderItemMapper::toResponseDto)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional(readOnly = true)
//    public List<OrderItemResponse> getByOrderId(Long orderId) {
//        return orderItemRepository.findByOrderId(orderId).stream()
//                .map(orderItemMapper::toResponseDto)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional
//    public OrderItemResponse update(Long id, OrderItemRequest orderItemRequest) {
//        validateRequest(orderItemRequest);
//
//        OrderItem orderItem = getOrderDetailEntityById(id);
//
//        if (orderItem.getOrder().getStatus() != OrderStatus.NEW) {
//            throw new CommonBackendException(
//                    "Cannot modify products in order with status: " + orderItem.getOrder().getStatus(),
//                    HttpStatus.BAD_REQUEST);
//        }
//
//        orderItem.setQuantity(orderItemRequest.quantity());
//        OrderItem saved = orderItemRepository.save(orderItem);
//        return orderItemMapper.toResponseDto(saved);
//    }
//
//    @Transactional
//    public void delete(Long id) {
//        OrderItem orderItem = getOrderDetailEntityById(id);
//
//        if (orderItem.getOrder().getStatus() != OrderStatus.NEW) {
//            throw new CommonBackendException("Cannot remove products from order with status: "
//                    + orderItem.getOrder().getStatus(), HttpStatus.BAD_REQUEST);
//        }
//        orderItemRepository.delete(orderItem);
//    }
//
//    private OrderItem getOrderDetailEntityById(Long id) {
//        return orderItemRepository.findById(id).orElseThrow(() ->
//                new CommonBackendException("OrderItem not found with id: " + id, HttpStatus.NOT_FOUND));
//    }
//
//    private void validateOrderForAddingProducts(Order order) {
//        if (order == null) {
//            throw new CommonBackendException(
//                    "Order must be specified", HttpStatus.BAD_REQUEST);
//        }
//
//        if (order.getStatus() != OrderStatus.NEW) {
//            throw new CommonBackendException(
//                    "Cannot add products to order with status: " + order.getStatus(), HttpStatus.BAD_REQUEST);
//        }
//    }
//
//    private void validateRequest(OrderItemRequest request) {
//        if (request.quantity() == null || request.quantity() <= 0) {
//            throw new CommonBackendException(
//                    "Quantity must be positive, got: " + request.quantity(), HttpStatus.BAD_REQUEST);
//        }
//    }
//
//    private void validateProductNotInActiveOrder(Product product) {
//        List<OrderStatus> completedStatuses = List.of(OrderStatus.COMPLETED, OrderStatus.CANCELED);
//
//        boolean productInActiveOrder = orderItemRepository.existsByProductAndOrderStatusNotIn(product,
//                completedStatuses);
//
//        if (productInActiveOrder) {
//            throw new CommonBackendException(
//                    "Product is already in another active order", HttpStatus.CONFLICT);
//        }
//    }
//}
