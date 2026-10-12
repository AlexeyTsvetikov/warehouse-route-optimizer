package ru.tsvetikov.warehouse.router.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import ru.tsvetikov.warehouse.router.event.OrderProcessingStartedEvent;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Order;
import ru.tsvetikov.warehouse.router.model.db.repository.OrderRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderItemRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderWithItemsRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderResponse;
import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;
import ru.tsvetikov.warehouse.router.model.enums.OrderType;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;
import ru.tsvetikov.warehouse.router.model.mapper.OrderMapper;
import ru.tsvetikov.warehouse.router.testdata.OrderItemTestData;
import ru.tsvetikov.warehouse.router.testdata.OrderTestData;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private StockService stockService;

    @Mock
    private WarehouseTaskService warehouseTaskService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCreateEmptyOrder() {
        OrderRequest request = OrderTestData.request();
        Order entity = OrderTestData.entityBuilder().id(1L).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(orderRepository.existsByOrderNumber("ORD-00001")).thenReturn(false);
        when(orderMapper.toEntity(request)).thenReturn(entity);
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.createEmptyOrder(request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getOrderNumber()).isEqualTo("ORD-00001");
        verify(orderRepository).existsByOrderNumber("ORD-00001");
        verify(orderRepository).findMaxId();
        verify(orderRepository).save(entity);
    }

    @Test
    void shouldThrowWhenFailGenerateUniqueNumber() {
        OrderRequest request = OrderTestData.request();

        when(orderRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(orderRepository.existsByOrderNumber("ORD-00001")).thenReturn(true);

        assertThatThrownBy(() -> orderService.createEmptyOrder(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Failed to generate unique order number after 3 attempts");

        verify(orderRepository, times(3)).existsByOrderNumber("ORD-00001");
        verify(orderRepository, times(3)).findMaxId();
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
    }

    @Test
    void shouldCreateOrderWithItems() {
        OrderWithItemsRequest request = OrderTestData.requestWithItems();
        Order entity = OrderTestData.entityBuilder().id(1L).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(orderRepository.existsByOrderNumber("ORD-00001")).thenReturn(false);
        when(orderMapper.toEntity(request.order())).thenReturn(entity);
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.createOrderWithItems(request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getOrderNumber()).isEqualTo("ORD-00001");
        verify(orderRepository).existsByOrderNumber("ORD-00001");
        verify(orderRepository).findMaxId();
        verify(orderItemService).create(eq("ORD-00001"), any(OrderItemRequest.class));
        verify(orderRepository).save(entity);
    }

    @Test
    void shouldCreateOrderWithItemsWhenRequestItemsIsNull() {
        OrderWithItemsRequest request = OrderTestData.requestWithoutItems();
        Order entity = OrderTestData.entityBuilder().id(1L).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(orderRepository.existsByOrderNumber("ORD-00001")).thenReturn(false);
        when(orderMapper.toEntity(request.order())).thenReturn(entity);
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.createOrderWithItems(request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getOrderNumber()).isEqualTo("ORD-00001");
        verify(orderRepository).existsByOrderNumber("ORD-00001");
        verify(orderRepository).findMaxId();
        verify(orderItemService, never()).create(eq("ORD-00001"), any(OrderItemRequest.class));
        verify(orderRepository).save(entity);
    }

    @Test
    void shouldCreateOrderWithItemsWhenRequestItemsIsEmptyList() {
        OrderWithItemsRequest request = OrderTestData.requestWithItems(List.of());
        Order entity = OrderTestData.entityBuilder().id(1L).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(orderRepository.existsByOrderNumber("ORD-00001")).thenReturn(false);
        when(orderMapper.toEntity(request.order())).thenReturn(entity);
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.createOrderWithItems(request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getOrderNumber()).isEqualTo("ORD-00001");
        verify(orderRepository).existsByOrderNumber("ORD-00001");
        verify(orderRepository).findMaxId();
        verify(orderItemService, never()).create(eq("ORD-00001"), any(OrderItemRequest.class));
        verify(orderRepository).save(entity);
    }

    @Test
    void shouldThrowWhenFailGenerateUniqueNumberOnCreateOrderWithItems() {
        OrderWithItemsRequest request = OrderTestData.requestWithItems();


        when(orderRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(orderRepository.existsByOrderNumber("ORD-00001")).thenReturn(true);

        assertThatThrownBy(() -> orderService.createOrderWithItems(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Failed to generate unique order number after 3 attempts");

        verifyNoInteractions(orderMapper);
        verifyNoInteractions(orderItemService);
        verify(orderRepository, times(3)).findMaxId();
        verify(orderRepository, times(3)).existsByOrderNumber("ORD-00001");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldReturnOrderByNumber() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.getByNumber(orderNumber);

        assertThat(result).isEqualTo(response);
        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderMapper).toResponseDto(entity);
    }

    @Test
    void shouldThrowOrderNotFoundOnGetByOrderNumber() {
        String orderNumber = "ORD-00001";

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getByNumber(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order with number '%s' not found", orderNumber);

        verifyNoInteractions(orderMapper);
        verify(orderRepository).findByOrderNumber(orderNumber);
    }

    @Test
    void shouldReturnAllOrders() {
        Order entity = OrderTestData.entityBuilder().id(1L).build();
        OrderResponse response = OrderTestData.response();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        Page<OrderResponse> result = orderService.getAll(0, 10, "createdAt", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(orderRepository).findAll(any(Pageable.class));
        verify(orderMapper).toResponseDto(entity);

    }

    @Test
    void shouldReturnAllOrdersFilteredByStatus() {
        Order entity = OrderTestData.entityBuilder().id(1L).build();
        OrderResponse response = OrderTestData.response();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(entity), pageable, 1);
        List<OrderStatus> statuses = List.of(OrderStatus.NEW, OrderStatus.PROCESSING, OrderStatus.COMPLETED);

        when(orderRepository.findByStatusIn(eq(statuses), any(Pageable.class))).thenReturn(page);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        Page<OrderResponse> result = orderService.getByStatuses(statuses, 0, 10, "createdAt", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(orderRepository).findByStatusIn(eq(statuses), any(Pageable.class));
        verify(orderMapper).toResponseDto(entity);
    }

    @Test
    void shouldReturnAllOrdersFilteredByQuery() {
        String query = "ORD";
        Order entity = OrderTestData.entityBuilder().id(1L).build();
        OrderResponse response = OrderTestData.response();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(orderRepository.search(eq(query), any(Pageable.class))).thenReturn(page);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        Page<OrderResponse> result = orderService.search(query, 0, 10, "createdAt", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(orderRepository).search(eq(query), any(Pageable.class));
        verify(orderMapper).toResponseDto(entity);
    }

    @Test
    void shouldUpdateOrder() {
        String orderNumber = "ORD-00001";
        OrderRequest request = OrderTestData.request();
        Order entity = OrderTestData.entityBuilder().id(1L).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.update(orderNumber, request);

        assertThat(result).isEqualTo(response);
        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderMapper).updateEntityFromDto(request, entity);
        verify(orderRepository).save(entity);
    }

    @Test
    void shouldThrowWhenOrderNotFoundOnUpdate() {
        String orderNumber = "ORD-00001";
        OrderRequest request = OrderTestData.request();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.update(orderNumber, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order with number '%s' not found", orderNumber);

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
    }

    @Test
    void shouldThrowWhenOrderStatusNotNew() {
        String orderNumber = "ORD-00001";
        OrderRequest request = OrderTestData.request();
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.PROCESSING).build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderService.update(orderNumber, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot update order with status: %s", entity.getStatus());

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(entity);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void shouldDeleteOrder() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));

        orderService.delete(orderNumber);

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository).delete(entity);
    }

    @Test
    void shouldThrowWhenOrderNotFoundOnDelete() {
        String orderNumber = "ORD-00001";

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.delete(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order with number '%s' not found", orderNumber);

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).delete(any());
    }

    @Test
    void shouldThrowWhenOrderStatusIsNotNew() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.PROCESSING).build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderService.delete(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot delete order with status: %s", entity.getStatus());

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).delete(any());
    }

    @Test
    void shouldStartProcessingOrder() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).build())).build();
        OrderResponse response = OrderTestData.response(OrderStatus.PROCESSING);

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.startProcessing(orderNumber);

        assertThat(result).isEqualTo(response);
        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository).save(entity);
        verify(eventPublisher).publishEvent(any(OrderProcessingStartedEvent.class));
    }

    @Test
    void shouldThrowWhenOrderNotFoundOnStartProcessing() {
        String orderNumber = "ORD-00001";

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.startProcessing(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order with number '%s' not found", orderNumber);

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldThrowWhenOrderIsEmpty() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderService.startProcessing(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot start processing empty order");

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldThrowWhenOrderStatusIsNotNewOnStartProcessing() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.PROCESSING).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).build())).build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderService.startProcessing(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Can only start processing NEW orders");

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldCompleteInboundOrder() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.PROCESSING).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(10).build())).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.completeInboundOrder(orderNumber);

        assertThat(result).isEqualTo(response);
        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository).save(entity);
    }

    @Test
    void shouldPartlyCompleteInboundOrder() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.PROCESSING).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(5).build())).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.completeInboundOrder(orderNumber);

        assertThat(result).isEqualTo(response);
        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository).save(entity);
    }

    @Test
    void shouldThrowWhenOrderNotFoundOnCompleteInboundOrder() {
        String orderNumber = "ORD-00001";

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.completeInboundOrder(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order with number '%s' not found", orderNumber);

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
    }

    @Test
    void shouldThrowWhenOrderStatusIsNotProcessing() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.COMPLETED).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(10).build())).build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderService.completeInboundOrder(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Can only complete PROCESSING orders");

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
    }

    @Test
    void shouldThrowWhenOrderTypeIsNotInbound() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.PROCESSING).type(OrderType.OUTBOUND)
                .orderItems(List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(10).build())).build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderService.completeInboundOrder(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("This method is only for INBOUND orders");

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
    }

    @Test
    void shouldCompleteOrder() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.PROCESSING).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(10).build())).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));
        when(warehouseTaskService.areAllTasksCompletedForOrder(orderNumber, WarehouseTaskType.RECEIVING)).thenReturn(true);
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.completeOrder(orderNumber);

        assertThat(result).isEqualTo(response);
        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(warehouseTaskService).areAllTasksCompletedForOrder(orderNumber, WarehouseTaskType.RECEIVING);
        verify(stockService, never()).releaseReserved(anyString(), anyInt());
        verify(orderRepository).save(entity);
    }

    @Test
    void shouldThrowWhenOrderNotFoundOnCompleteOrder() {
        String orderNumber = "ORD-00001";

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.completeOrder(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order with number '%s' not found", orderNumber);

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
        verifyNoInteractions(warehouseTaskService);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldThrowWhenOrderStatusIsNotProcessingOnCompleteOrder() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.COMPLETED).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(10).build())).build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderService.completeOrder(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Can only complete PROCESSING orders");

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
        verifyNoInteractions(warehouseTaskService);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldThrowWhenOrderIsNotFullyCompleted() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.PROCESSING).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(10).build())).build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));
        when(warehouseTaskService.areAllTasksCompletedForOrder(orderNumber, WarehouseTaskType.RECEIVING)).thenReturn(false);

        assertThatThrownBy(() -> orderService.completeOrder(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot complete order: not all %s tasks are completed", WarehouseTaskType.RECEIVING);

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(any());
        verify(warehouseTaskService).areAllTasksCompletedForOrder(orderNumber, WarehouseTaskType.RECEIVING);
        verifyNoInteractions(stockService);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void shouldReleaseReservedStockOnPartialCollection() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.PROCESSING).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(5).build())).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));
        when(warehouseTaskService.areAllTasksCompletedForOrder(orderNumber, WarehouseTaskType.RECEIVING)).thenReturn(true);
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.completeOrder(orderNumber);

        assertThat(result).isEqualTo(response);
        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(warehouseTaskService).areAllTasksCompletedForOrder(orderNumber, WarehouseTaskType.RECEIVING);
        verify(stockService).releaseReserved("BRP-ENG-001", 5);
        verify(orderRepository).save(entity);
    }

    @Test
    void shouldUsePickingTaskTypeForOutboundOrder() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.PROCESSING).type(OrderType.OUTBOUND)
                .orderItems(List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(10).build())).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));
        when(warehouseTaskService.areAllTasksCompletedForOrder(orderNumber, WarehouseTaskType.PICKING)).thenReturn(true);
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.completeOrder(orderNumber);

        assertThat(result).isEqualTo(response);
        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(warehouseTaskService).areAllTasksCompletedForOrder(orderNumber, WarehouseTaskType.PICKING);
        verify(stockService, never()).releaseReserved(anyString(), anyInt());
        verify(orderRepository).save(entity);
    }

    @Test
    void shouldCancelOrder() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.PROCESSING).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(10).build())).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));
        when(warehouseTaskService.hasActiveTasksForOrder(orderNumber)).thenReturn(false);
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.cancelOrder(orderNumber);

        assertThat(result).isEqualTo(response);
        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(warehouseTaskService).hasActiveTasksForOrder(orderNumber);
        verify(stockService, never()).releaseReserved(anyString(), anyInt());
        verify(orderRepository).save(entity);
    }

    @Test
    void shouldThrowWhenOrderNotFoundOnCancelOrder() {
        String orderNumber = "ORD-00001";

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order with number '%s' not found", orderNumber);

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
        verifyNoInteractions(warehouseTaskService);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldThrowWhenOrderStatusIsCompleted() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.COMPLETED).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(10).build())).build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderService.cancelOrder(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot cancel COMPLETED order");

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
        verifyNoInteractions(warehouseTaskService);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldThrowWhenOrderStatusIsCancelled() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.CANCELLED).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(10).build())).build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderService.cancelOrder(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order is already cancelled");

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
        verifyNoInteractions(warehouseTaskService);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldCancelOrderWithActiveWarehouseTasks() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.PROCESSING).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(5).build())).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));
        when(warehouseTaskService.hasActiveTasksForOrder(orderNumber)).thenReturn(true);
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.cancelOrder(orderNumber);

        assertThat(result).isEqualTo(response);
        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(warehouseTaskService).hasActiveTasksForOrder(orderNumber);
        verify(stockService).releaseReserved("BRP-ENG-001", 5);
        verify(orderRepository).save(entity);
    }

    @Test
    void shouldCancelNewOrder() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.NEW).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(10).build())).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.cancelOrder(orderNumber);

        assertThat(result).isEqualTo(response);
        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderRepository).save(entity);
        verifyNoInteractions(warehouseTaskService);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldCancelOrderWithActiveTasksButNoReserved() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).status(OrderStatus.PROCESSING).orderItems(
                List.of(OrderItemTestData.entityBuilder().id(1L).collectedQuantity(10).build())).build();
        OrderResponse response = OrderTestData.response();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));
        when(warehouseTaskService.hasActiveTasksForOrder(orderNumber)).thenReturn(true);
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponseDto(entity)).thenReturn(response);

        OrderResponse result = orderService.cancelOrder(orderNumber);

        assertThat(result).isEqualTo(response);
        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(warehouseTaskService).hasActiveTasksForOrder(orderNumber);
        verify(stockService, never()).releaseReserved(anyString(), anyInt());
        verify(orderRepository).save(entity);
    }
}