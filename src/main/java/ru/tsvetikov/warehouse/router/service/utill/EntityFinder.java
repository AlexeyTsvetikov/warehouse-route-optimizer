package ru.tsvetikov.warehouse.router.service.utill;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Order;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.db.entity.RackCell;
import ru.tsvetikov.warehouse.router.model.db.entity.User;
import ru.tsvetikov.warehouse.router.model.db.repository.OrderRepository;
import ru.tsvetikov.warehouse.router.model.db.repository.ProductRepository;
import ru.tsvetikov.warehouse.router.model.db.repository.RackCellRepository;
import ru.tsvetikov.warehouse.router.model.db.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class EntityFinder {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RackCellRepository rackCellRepository;


    public Product findProductByTrackingNumber(String trackingNumber) {
        return productRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new CommonBackendException(
                        "Product not found with tracking number: " + trackingNumber, HttpStatus.NOT_FOUND));
    }

    public RackCell findCellByCode(String cellCode) {
        return rackCellRepository.findByCellCode(cellCode)
                .orElseThrow(() -> new CommonBackendException(
                        "Cell not found with code: " + cellCode, HttpStatus.NOT_FOUND));
    }

    public User findUserByUsername(String username) {
        if (username == null) return null;

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new CommonBackendException(
                        "User not found with username: " + username, HttpStatus.NOT_FOUND));
    }

    public Order findOrderByNumber(String orderNumber) {
        if (orderNumber == null) return null;

        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new CommonBackendException("Order not found with number: "
                        + orderNumber, HttpStatus.NOT_FOUND));
    }
}
