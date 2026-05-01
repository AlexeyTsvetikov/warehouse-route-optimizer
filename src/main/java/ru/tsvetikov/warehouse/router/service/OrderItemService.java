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
import ru.tsvetikov.warehouse.router.event.OrderItemCompletedEvent;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Order;
import ru.tsvetikov.warehouse.router.model.db.entity.OrderItem;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.db.repository.OrderItemRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderItemRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderItemResponse;
import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;
import ru.tsvetikov.warehouse.router.model.enums.OrderType;
import ru.tsvetikov.warehouse.router.model.mapper.OrderItemMapper;
import ru.tsvetikov.warehouse.router.utils.PaginationUtils;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final OrderItemMapper orderItemMapper;
    private final StockService stockService;
    private final ProductService productService;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderQueryService orderQueryService;

    @Transactional
    public OrderItemResponse create(String orderNumber, OrderItemRequest request) {
        Order order = findOrderByNumberOrThrow(orderNumber);
        validateOrderCanBeModified(order);

        Product product = findProductBySkuOrThrow(request.productSku());
        checkProductNotInOrder(order, product);

        if (order.getType() == OrderType.OUTBOUND) {
            stockService.checkAvailability(request.productSku(), request.quantity());
        }

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

    @Transactional(readOnly = true)
    public List<OrderItemResponse> getByOrderForWeb(String orderNumber) {
        Order order = findOrderByNumberOrThrow(orderNumber);
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()); // сортировка по id
        return items.stream()
                .map(orderItemMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderItemResponse update(String orderNumber, Long id, OrderItemRequest request) {
        OrderItem item = findOrderItemOrThrow(id);
        Order order = findOrderByNumberOrThrow(orderNumber);

        validateOrderItemBelongsToOrder(item, orderNumber);
        validateOrderCanBeModified(item.getOrder());

        boolean productChanged = request.productSku() != null && !request.productSku().equals(item.getProduct().getSku());

        if (productChanged) {
            Product newProduct = findProductBySkuOrThrow(request.productSku());
            checkProductNotInOrder(item.getOrder(), newProduct);

            if (order.getType() == OrderType.OUTBOUND) {
                stockService.checkAvailability(request.productSku(), request.quantity());
            }
            item.setProduct(newProduct);
        }

        if (request.quantity() != null && !request.quantity().equals(item.getQuantity())) {
            int newQty = request.quantity();

            if (newQty <= 0) {
                throw new CommonBackendException("Quantity must be positive", HttpStatus.BAD_REQUEST);
            }

            stockService.checkAvailability(item.getProduct().getSku(), newQty);
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
    public void addCollectedQuantity(Order order, Product product, int quantityToAdd) {
        OrderItem orderItem = orderItemRepository.findByOrderAndProduct(order, product)
                .orElseThrow(() -> new CommonBackendException(
                        "OrderItem not found for order " + order.getOrderNumber() +
                        " and product " + product.getSku(), HttpStatus.NOT_FOUND));

        int newCollected = orderItem.getCollectedQuantity() + quantityToAdd;
        updateCollectedQuantity(order.getOrderNumber(), orderItem.getId(), newCollected);
    }

    @Transactional(readOnly = true)
    public int getRemainingQuantity(Order order, Product product) {
        OrderItem orderItem = orderItemRepository.findByOrderAndProduct(order, product)
                .orElseThrow(() -> new CommonBackendException("OrderItem not found", HttpStatus.NOT_FOUND));
        return orderItem.getQuantity() - orderItem.getCollectedQuantity();
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

        if (updated.isFullyCollected()) {
            eventPublisher.publishEvent(new OrderItemCompletedEvent(updated));
            log.info("Order item {} fully collected. Event published.", id);
        }

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
        return orderQueryService.getOrderEntityByNumber(orderNumber);
    }

    private Product findProductBySkuOrThrow(String sku) {
        return productService.getBySku(sku);
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