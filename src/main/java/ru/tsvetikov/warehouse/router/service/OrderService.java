//package ru.tsvetikov.warehouse.router.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
//import ru.tsvetikov.warehouse.router.model.db.entity.Order;
//import ru.tsvetikov.warehouse.router.model.db.repository.OrderRepository;
//import ru.tsvetikov.warehouse.router.model.dto.request.OrderRequest;
//import ru.tsvetikov.warehouse.router.model.dto.response.OrderResponse;
//import ru.tsvetikov.warehouse.router.model.mapper.OrderMapper;
//
//import java.util.List;
//import java.util.stream.Collectors;

//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class OrderService {
//    private final OrderRepository orderRepository;
//    private final OrderMapper orderMapper;
//
//    @Transactional
//    public OrderResponse create(OrderRequest request) {
//        if (orderRepository.findByOrderNumber(request.orderNumber()).isPresent()) {
//            throw new CommonBackendException(
//                    "Order with number already exists: " + request.orderNumber(), HttpStatus.CONFLICT);
//        }
//
//        Order order = orderMapper.toEntity(request);
//        Order saved = orderRepository.save(order);
//
//        return orderMapper.toResponseDto(saved);
//    }
//
//    @Transactional(readOnly = true)
//    public OrderResponse getById(Long id) {
//        Order order = orderRepository.findById(id)
//                .orElseThrow(() -> new CommonBackendException("Order not found with id: " + id, HttpStatus.NOT_FOUND));
//        return orderMapper.toResponseDto(order);
//    }
//
//    @Transactional(readOnly = true)
//    public List<OrderResponse> getAll() {
//        return orderRepository.findAll().stream()
//                .map(orderMapper::toResponseDto)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional
//    public void delete(Long id) {
//        if (!orderRepository.existsById(id)) {
//            throw new CommonBackendException("Order not found with id: " + id, HttpStatus.NOT_FOUND);
//        }
//
//        orderRepository.deleteById(id);
//    }
//
//    @Transactional
//    public OrderResponse update(Long id, OrderRequest request) {
//        Order existing = orderRepository.findById(id)
//                .orElseThrow(() -> new CommonBackendException("Order not found with id: " + id, HttpStatus.NOT_FOUND));
//
//        if (request.orderNumber() != null && !request.orderNumber().equals(existing.getOrderNumber())) {
//            if (orderRepository.findByOrderNumber(request.orderNumber()).isPresent()) {
//                throw new CommonBackendException("Order with number already exists: " + request.orderNumber(),
//                        HttpStatus.CONFLICT);
//            }
//        }
//
//        orderMapper.updateEntityFromDto(request, existing);
//        Order updated = orderRepository.save(existing);
//
//        return orderMapper.toResponseDto(updated);
//    }
//}
