package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.event.OrderProcessingStartedEvent;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Order;
import ru.tsvetikov.warehouse.router.model.db.entity.OrderItem;
import ru.tsvetikov.warehouse.router.model.db.repository.OrderRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderItemRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderWithItemsRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderResponse;
import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;
import ru.tsvetikov.warehouse.router.model.enums.OrderType;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;
import ru.tsvetikov.warehouse.router.model.mapper.OrderMapper;
import ru.tsvetikov.warehouse.router.utils.PaginationUtils;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderItemService orderItemService;
    private final StockService stockService;
    private final ApplicationEventPublisher eventPublisher;
    private final WarehouseTaskService warehouseTaskService;


    @Transactional
    public OrderResponse createEmptyOrder(OrderRequest request) {
        String orderNumber = generateUniqueOrderNumber();
        Order order = orderMapper.toEntity(request);
        order.setOrderNumber(orderNumber);
        Order saved = orderRepository.save(order);
        log.info("Created empty order: {}", saved.getOrderNumber());
        return orderMapper.toResponseDto(saved);
    }


    @Transactional
    public OrderResponse createOrderWithItems(OrderWithItemsRequest request) {
        String orderNumber = generateUniqueOrderNumber();
        Order order = orderMapper.toEntity(request.order());
        order.setOrderNumber(orderNumber);
        Order savedOrder = orderRepository.save(order);

        if (request.items() != null && !request.items().isEmpty()) {
            addItemsToOrder(savedOrder, request.items());
        }

        log.info("Created order {} with {} items",
                savedOrder.getOrderNumber(),
                request.items() != null ? request.items().size() : 0);
        return orderMapper.toResponseDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getByNumber(String orderNumber) {
        Order order = findOrderByNumberOrThrow(orderNumber);
        return orderMapper.toResponseDto(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAll(int page, int size, String sort, Sort.Direction orderDirection) {
        Pageable pageable = PaginationUtils.getPageRequest(page, size, sort, orderDirection);
        return orderRepository.findAll(pageable)
                .map(orderMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> search(String query, int page, int size, String sort, Sort.Direction orderDirection) {
        Pageable pageable = PaginationUtils.getPageRequest(page, size, sort, orderDirection);
        return orderRepository.search(query, pageable)
                .map(orderMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getByStatuses(List<OrderStatus> statuses, int page, int size, String sort, Sort.Direction orderDirection) {
        Pageable pageable = PaginationUtils.getPageRequest(page, size, sort, orderDirection);
        return orderRepository.findByStatusIn(statuses, pageable)
                .map(orderMapper::toResponseDto);
    }

    @Transactional
    public OrderResponse update(String orderNumber, OrderRequest request) {
        Order order = findOrderByNumberOrThrow(orderNumber);
        validateOrderCanBeUpdated(order);

        orderMapper.updateEntityFromDto(request, order);
        Order updated = orderRepository.save(order);

        log.info("Updated order: {}", orderNumber);
        return orderMapper.toResponseDto(updated);
    }

    @Transactional
    public void delete(String orderNumber) {
        Order order = findOrderByNumberOrThrow(orderNumber);

        validateOrderCanBeUpdated(order);

        orderRepository.delete(order);
        log.info("Deleted order: {}", orderNumber);
    }

    @Transactional
    public OrderResponse startProcessing(String orderNumber) {
        Order order = findOrderByNumberOrThrow(orderNumber);

        if (order.getStatus() != OrderStatus.NEW) {
            throw new CommonBackendException(
                    "Can only start processing NEW orders", HttpStatus.BAD_REQUEST);
        }

        if (order.getOrderItems().isEmpty()) {
            throw new CommonBackendException(
                    "Cannot start processing empty order", HttpStatus.BAD_REQUEST);
        }

        order.setStatus(OrderStatus.PROCESSING);
        Order updated = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderProcessingStartedEvent(orderNumber));
        log.info("Started processing order: {}", orderNumber);
        return orderMapper.toResponseDto(updated);
    }

    @Transactional
    public OrderResponse completeOrder(String orderNumber) {
        Order order = findOrderByNumberOrThrow(orderNumber);

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new CommonBackendException("Can only complete PROCESSING orders", HttpStatus.BAD_REQUEST);
        }

        WarehouseTaskType requiredTaskType = getRequiredTaskType(order);

        boolean allTasksCompleted = warehouseTaskService.areAllTasksCompletedForOrder(orderNumber, requiredTaskType);

        if (!allTasksCompleted) {
            throw new CommonBackendException(
                    String.format("Cannot complete order: not all %s tasks are completed", requiredTaskType),
                    HttpStatus.BAD_REQUEST);
        }

        for (OrderItem item : order.getOrderItems()) {
            if (!item.isFullyCollected()) {
                log.warn("Order {} item {} has shortage: ordered {}, collected {}",
                        orderNumber, item.getProduct().getSku(),
                        item.getQuantity(), item.getCollectedQuantity());
            }
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(Instant.now());
        Order updated = orderRepository.save(order);

        log.info("Completed order: {}", orderNumber);
        return orderMapper.toResponseDto(updated);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderNumber) {
        Order order = findOrderByNumberOrThrow(orderNumber);

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new CommonBackendException("Cannot cancel COMPLETED order", HttpStatus.BAD_REQUEST);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new CommonBackendException("Order is already cancelled", HttpStatus.BAD_REQUEST);
        }

        for (OrderItem item : order.getOrderItems()) {
            int reservedQuantity = item.getQuantity() - item.getCollectedQuantity();
            if (reservedQuantity > 0) {
                stockService.releaseReserved(item.getProduct().getSku(), reservedQuantity);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        log.info("Cancelled order: {}", orderNumber);
        return orderMapper.toResponseDto(updated);
    }

    private String generateOrderNumber() {
        Long maxId = orderRepository.findMaxId().orElse(0L);
        return String.format("ORD-%05d", maxId + 1);
    }

    private String generateUniqueOrderNumber() {
        String orderNumber = generateOrderNumber();
        int attempts = 0;
        while (orderRepository.existsByOrderNumber(orderNumber) && attempts < 3) {
            orderNumber = generateOrderNumber();
            attempts++;
        }
        if (orderRepository.existsByOrderNumber(orderNumber)) {
            throw new CommonBackendException("Failed to generate unique order number after 3 attempts", HttpStatus.CONFLICT);
        }
        return orderNumber;
    }

    private Order findOrderByNumberOrThrow(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Order with number '%s' not found", orderNumber), HttpStatus.NOT_FOUND));
    }

    private void validateOrderCanBeUpdated(Order order) {
        if (order.getStatus() != OrderStatus.NEW) {
            throw new CommonBackendException(
                    String.format("Cannot update order with status: %s", order.getStatus()), HttpStatus.BAD_REQUEST);
        }
    }

    private void addItemsToOrder(Order order, List<OrderItemRequest> itemRequests) {
        for (OrderItemRequest itemRequest : itemRequests) {
            orderItemService.create(order.getOrderNumber(), itemRequest);
        }
    }

    private WarehouseTaskType getRequiredTaskType(Order order) {
        return order.getType() == OrderType.INBOUND ? WarehouseTaskType.RECEIVING : WarehouseTaskType.PICKING;
    }
}