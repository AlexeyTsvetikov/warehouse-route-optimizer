package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Order;
import ru.tsvetikov.warehouse.router.model.db.entity.OrderDetail;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.db.repository.OrderDetailRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderDetailRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderDetailResponse;
import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;
import ru.tsvetikov.warehouse.router.model.mapper.OrderDetailMapper;
import ru.tsvetikov.warehouse.router.service.utill.EntityFinder;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDetailService {
    private final OrderDetailRepository orderDetailRepository;
    private final OrderDetailMapper orderDetailMapper;
    private final EntityFinder entityFinder;

    @Transactional
    public OrderDetailResponse create(OrderDetailRequest request) {
        validateRequest(request);

        Order order = entityFinder.findOrderByNumber(request.orderNumber());
        Product product = entityFinder.findProductByTrackingNumber(request.trackingNumber());

        validateOrderForAddingProducts(order);
        validateProductNotInActiveOrder(product);

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setQuantity(request.quantity());
        orderDetail.setOrder(order);
        orderDetail.setProduct(product);

        OrderDetail saved = orderDetailRepository.save(orderDetail);
        return orderDetailMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getById(Long id) {
        OrderDetail orderDetail = getOrderDetailEntityById(id);
        return orderDetailMapper.toResponseDto(orderDetail);
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getAll() {
        return orderDetailRepository.findAll().stream()
                .map(orderDetailMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getByOrderId(Long orderId) {
        return orderDetailRepository.findByOrderId(orderId).stream()
                .map(orderDetailMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDetailResponse update(Long id, OrderDetailRequest orderDetailRequest) {
        validateRequest(orderDetailRequest);

        OrderDetail orderDetail = getOrderDetailEntityById(id);

        if (orderDetail.getOrder().getStatus() != OrderStatus.FORMING) {
            throw new CommonBackendException(
                    "Cannot modify products in order with status: " + orderDetail.getOrder().getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        orderDetail.setQuantity(orderDetailRequest.quantity());
        OrderDetail saved = orderDetailRepository.save(orderDetail);
        return orderDetailMapper.toResponseDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        OrderDetail orderDetail = getOrderDetailEntityById(id);

        if (orderDetail.getOrder().getStatus() != OrderStatus.FORMING) {
            throw new CommonBackendException("Cannot remove products from order with status: "
                    + orderDetail.getOrder().getStatus(), HttpStatus.BAD_REQUEST);
        }
        orderDetailRepository.delete(orderDetail);
    }

    private OrderDetail getOrderDetailEntityById(Long id) {
        return orderDetailRepository.findById(id).orElseThrow(() ->
                new CommonBackendException("OrderDetail not found with id: " + id, HttpStatus.NOT_FOUND));
    }

    private void validateOrderForAddingProducts(Order order) {
        if (order == null) {
            throw new CommonBackendException(
                    "Order must be specified", HttpStatus.BAD_REQUEST);
        }

        if (order.getStatus() != OrderStatus.FORMING) {
            throw new CommonBackendException(
                    "Cannot add products to order with status: " + order.getStatus(), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateRequest(OrderDetailRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new CommonBackendException(
                    "Quantity must be positive, got: " + request.quantity(), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateProductNotInActiveOrder(Product product) {
        List<OrderStatus> completedStatuses = List.of(OrderStatus.CANCELLED, OrderStatus.SHIPPED);

        boolean productInActiveOrder = orderDetailRepository.existsByProductAndOrderStatusNotIn(product,
                completedStatuses);

        if (productInActiveOrder) {
            throw new CommonBackendException(
                    "Product is already in another active order", HttpStatus.CONFLICT);
        }
    }
}
