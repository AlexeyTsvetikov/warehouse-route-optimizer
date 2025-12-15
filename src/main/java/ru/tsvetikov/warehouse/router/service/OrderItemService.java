package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Order;
import ru.tsvetikov.warehouse.router.model.db.entity.OrderItem;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.db.repository.OrderItemRepository;
import ru.tsvetikov.warehouse.router.model.db.repository.OrderRepository;
import ru.tsvetikov.warehouse.router.model.db.repository.ProductRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderItemRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderItemResponse;
import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;
import ru.tsvetikov.warehouse.router.model.mapper.OrderItemMapper;
import ru.tsvetikov.warehouse.router.utils.PaginationUtils;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final OrderItemMapper orderItemMapper;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public OrderItemResponse create(String orderNumber, OrderItemRequest request) {
        Order order = findOrderByNumberOrThrow(orderNumber);
        validateOrderCanBeModified(order);

        Product product = findProductBySkuOrThrow(request.productSku());
        checkProductNotInOrder(order, product);

        //TODO: Проверка доступности товара на складе
        // checkProductAvailability(product, request.quantity());

        OrderItem item = orderItemMapper.toEntity(request);
        item.setOrder(order);
        item.setProduct(product);

        OrderItem saved = orderItemRepository.save(item);
        return orderItemMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public OrderItemResponse getById(String orderNumber, Long id) {
        OrderItem item = findOrderItemOrThrow(id);
        validateOrderItemBelongsToOrder(item, orderNumber);
        return orderItemMapper.toResponseDto(item);
    }

    @Transactional(readOnly = true)
    public Page<OrderItemResponse> getByOrder(String orderNumber, Integer page, Integer perPage,
                                              String sort, Sort.Direction orderDirection) {
        Order order = findOrderByNumberOrThrow(orderNumber);

        Pageable pageRequest = PaginationUtils.getPageRequest(page, perPage, sort, orderDirection);
        Page<OrderItem> items = orderItemRepository.findByOrderId(order.getId(), pageRequest);

        return items.map(orderItemMapper::toResponseDto);
    }

    @Transactional
    public OrderItemResponse update(String orderNumber, Long id, OrderItemRequest request) {
        OrderItem item = findOrderItemOrThrow(id);
        validateOrderItemBelongsToOrder(item, orderNumber);
        validateOrderCanBeModified(item.getOrder());

        if (request.productSku() != null && !request.productSku().equals(item.getProduct().getSku())) {
            Product newProduct = findProductBySkuOrThrow(request.productSku());
            checkProductNotInOrder(item.getOrder(), newProduct);
            //TODO: Проверка доступности товара на складе
            // checkProductAvailability(newProduct, request.quantity());
            item.setProduct(newProduct);
        }

        if (request.quantity() != null && request.quantity() <= 0) {
            throw new CommonBackendException("Quantity must be positive", HttpStatus.BAD_REQUEST);
        }

        orderItemMapper.updateEntityFromDto(request, item);
        OrderItem updated = orderItemRepository.save(item);
        return orderItemMapper.toResponseDto(updated);
    }

    @Transactional
    public void delete(String orderNumber, Long id) {
        OrderItem item = findOrderItemOrThrow(id);
        validateOrderItemBelongsToOrder(item, orderNumber);
        validateOrderCanBeModified(item.getOrder());

        orderItemRepository.delete(item);
    }

    @Transactional
    public OrderItemResponse updateCollectedQuantity(String orderNumber, Long id, Integer collectedQuantity) {
        OrderItem item = findOrderItemOrThrow(id);
        validateOrderItemBelongsToOrder(item, orderNumber);

        if (collectedQuantity < 0) {
            throw new CommonBackendException("Collected quantity cannot be negative", HttpStatus.BAD_REQUEST);
        }

        if (collectedQuantity > item.getQuantity()) {
            throw new CommonBackendException(
                    "Collected quantity cannot exceed ordered quantity", HttpStatus.BAD_REQUEST);
        }

        item.setCollectedQuantity(collectedQuantity);
        OrderItem updated = orderItemRepository.save(item);

        // TODO: При полном сборе товара - триггер для обновления статуса задачи
        // if (updated.isFullyCollected()) {
        //     warehouseTaskService.onOrderItemFullyCollected(updated);
        // }

        log.info("Updated collected quantity for item {}: {}/{}",
                id, collectedQuantity, item.getQuantity());
        return orderItemMapper.toResponseDto(updated);
    }

    private OrderItem findOrderItemOrThrow(Long id) {
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Order item with id: %s not found", id), HttpStatus.NOT_FOUND));
    }

    private Order findOrderByNumberOrThrow(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Order with number '%s' not found", orderNumber), HttpStatus.NOT_FOUND));
    }

    private Product findProductBySkuOrThrow(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Product with SKU '%s' not found", sku), HttpStatus.NOT_FOUND));
    }

    private void validateOrderCanBeModified(Order order) {
        if (order.getStatus() != OrderStatus.NEW) {
            throw new CommonBackendException(
                    String.format("Cannot modify order with status: %s", order.getStatus()), HttpStatus.BAD_REQUEST);
        }
    }

    private void checkProductNotInOrder(Order order, Product product) {
        if (orderItemRepository.existsByOrderIdAndProductId(order.getId(), product.getId())) {
            throw new CommonBackendException(
                    "Product already exists in this order", HttpStatus.CONFLICT);
        }
    }

    private void validateOrderItemBelongsToOrder(OrderItem orderItem, String expectedOrderNumber) {
        String actualOrderNumber = orderItem.getOrder().getOrderNumber();
        if (!expectedOrderNumber.equals(actualOrderNumber)) {
            throw new CommonBackendException(
                    String.format("Order item %d belongs to order '%s', not '%s'",
                            orderItem.getId(), actualOrderNumber, expectedOrderNumber), HttpStatus.NOT_FOUND);
        }
    }
}