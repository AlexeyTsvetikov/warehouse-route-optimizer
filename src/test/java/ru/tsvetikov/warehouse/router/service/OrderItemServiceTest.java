package ru.tsvetikov.warehouse.router.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
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
import ru.tsvetikov.warehouse.router.testdata.OrderItemTestData;
import ru.tsvetikov.warehouse.router.testdata.OrderTestData;
import ru.tsvetikov.warehouse.router.testdata.ProductTestData;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private ProductService productService;
    @Mock
    private StockService stockService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private OrderQueryService orderQueryService;

    @InjectMocks
    private OrderItemService orderItemService;

    @Test
    void shouldCreateOrderItem() {
        String orderNumber = "ORD-00001";
        OrderItemRequest request = OrderItemTestData.request();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        Order order = OrderTestData.entityBuilder().id(1L).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        OrderItemResponse response = OrderItemTestData.response();

        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);
        when(orderItemRepository.existsByOrderIdAndProductId(order.getId(), product.getId())).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(orderItemMapper.toEntity(request)).thenReturn(entity);
        when(orderItemRepository.save(entity)).thenReturn(entity);
        when(orderItemMapper.toResponseDto(entity)).thenReturn(response);

        OrderItemResponse result = orderItemService.create(orderNumber, request);

        assertThat(result).isEqualTo(response);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(productService).getBySku(request.productSku());
        verify(orderItemRepository).existsByOrderIdAndProductId(order.getId(), product.getId());
        verify(orderItemRepository).save(entity);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldCreateOrderItemWhenOrderTypeIsOutbound() {
        String orderNumber = "ORD-00001";
        OrderItemRequest request = OrderItemTestData.request();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        Order order = OrderTestData.entityBuilder().id(1L).type(OrderType.OUTBOUND).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        OrderItemResponse response = OrderItemTestData.response();

        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(orderItemRepository.existsByOrderIdAndProductId(order.getId(), product.getId())).thenReturn(false);
        when(orderItemMapper.toEntity(request)).thenReturn(entity);
        when(orderItemRepository.save(entity)).thenReturn(entity);
        when(orderItemMapper.toResponseDto(entity)).thenReturn(response);

        OrderItemResponse result = orderItemService.create(orderNumber, request);

        assertThat(result).isEqualTo(response);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(productService).getBySku(request.productSku());
        verify(orderItemRepository).existsByOrderIdAndProductId(order.getId(), product.getId());
        verify(stockService).checkAvailability(request.productSku(), request.quantity());
        verify(orderItemRepository).save(entity);
    }

    @Test
    void shouldThrowWhenOrderNotFoundOnCreateOrderItem() {
        String orderNumber = "ORD-00001";
        OrderItemRequest request = OrderItemTestData.request();

        when(orderQueryService.getOrderEntityByNumber(orderNumber))
                .thenThrow(new CommonBackendException("Order with number 'ORD-00001' not found", HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> orderItemService.create(orderNumber, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order with number '%s' not found", orderNumber);

        verifyNoInteractions(orderItemMapper);
        verifyNoInteractions(stockService);
        verifyNoInteractions(productService);
        verify(orderItemRepository, never()).save(any());
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
    }

    @Test
    void shouldThrowWhenOrderStatusIsNotNewOnOrderItemCreate() {
        String orderNumber = "ORD-00001";
        OrderItemRequest request = OrderItemTestData.request();
        Order order = OrderTestData.entityBuilder().id(1L).type(OrderType.OUTBOUND).status(OrderStatus.COMPLETED).build();

        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);

        assertThatThrownBy(() -> orderItemService.create(orderNumber, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot modify order with status: %s", order.getStatus());

        verifyNoInteractions(orderItemMapper);
        verifyNoInteractions(stockService);
        verifyNoInteractions(productService);
        verify(orderItemRepository, never()).save(any());
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
    }

    @Test
    void shouldThrowWhenProductAlreadyInOrderOnCreateOrderItem() {
        String orderNumber = "ORD-00001";
        OrderItemRequest request = OrderItemTestData.request();
        Order order = OrderTestData.entityBuilder().id(1L).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();

        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(orderItemRepository.existsByOrderIdAndProductId(order.getId(), product.getId())).thenReturn(true);

        assertThatThrownBy(() -> orderItemService.create(orderNumber, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Product already exists in this order");

        verifyNoInteractions(orderItemMapper);
        verifyNoInteractions(stockService);
        verify(productService).getBySku(request.productSku());
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void shouldReturnOrderItemById() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        OrderItemResponse response = OrderItemTestData.response();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderItemMapper.toResponseDto(entity)).thenReturn(response);

        OrderItemResponse result = orderItemService.getById(orderNumber, id);

        assertThat(result).isEqualTo(response);
        verify(orderItemRepository).findById(id);
    }

    @Test
    void shouldThrowWhenOrderItemNotFoundOnGetById() {
        String orderNumber = "ORD-00001";
        Long id = 1L;

        when(orderItemRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.getById(orderNumber, id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order item with id: %s not found", id);

        verifyNoInteractions(orderItemMapper);
        verify(orderItemRepository).findById(id);
    }

    @Test
    void shouldThrowWhenOrderItemNotBelongToOrder() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Order order = OrderTestData.entityBuilder().id(1L).orderNumber("ORD-00002").build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).order(order).build();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderItemService.getById(orderNumber, id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order item %d belongs to order '%s', not '%s'",
                        entity.getId(), entity.getOrder().getOrderNumber(), orderNumber);

        verifyNoInteractions(orderItemMapper);
        verify(orderItemRepository).findById(id);
    }

    @Test
    void shouldReturnPageOfOrderItemsByOrderNumber() {
        String orderNumber = "ORD-00001";
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        Order order = OrderTestData.entityBuilder().id(1L).build();
        OrderItemResponse response = OrderItemTestData.response();
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderItem> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);
        when(orderItemRepository.findByOrderId(eq(order.getId()), any(Pageable.class))).thenReturn(page);
        when(orderItemMapper.toResponseDto(entity)).thenReturn(response);

        Page<OrderItemResponse> result = orderItemService.getByOrder(orderNumber, 0, 10,
                "priority", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(orderItemRepository).findByOrderId(eq(order.getId()), any(Pageable.class));
        verify(orderItemMapper).toResponseDto(entity);
    }

    @Test
    void shouldThrowWhenOrderNotFoundOnGetByOrder() {
        String orderNumber = "ORD-00001";

        when(orderQueryService.getOrderEntityByNumber(orderNumber)).
                thenThrow(new CommonBackendException("Order with number 'ORD-00001' not found", HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> orderItemService.getByOrder(orderNumber, 0, 10,
                "priority", Sort.Direction.ASC))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order with number '%s' not found", orderNumber);

        verifyNoInteractions(orderItemMapper);
        verifyNoInteractions(orderItemRepository);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
    }

    @Test
    void shouldReturnPageOfOrderItemsByOrderForWeb() {
        String orderNumber = "ORD-00001";
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        Order order = OrderTestData.entityBuilder().id(1L).build();
        OrderItemResponse response = OrderItemTestData.response();

        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);
        when(orderItemRepository.findByOrderIdOrderByIdAsc(order.getId())).thenReturn(List.of(entity));
        when(orderItemMapper.toResponseDto(entity)).thenReturn(response);

        List<OrderItemResponse> result = orderItemService.getByOrderForWeb(orderNumber);

        assertThat(result.getFirst()).isEqualTo(response);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(orderItemRepository).findByOrderIdOrderByIdAsc(order.getId());
    }

    @Test
    void shouldThrowWhenOrderNotFoundOnGetByOrderForWeb() {
        String orderNumber = "ORD-00001";

        when(orderQueryService.getOrderEntityByNumber(orderNumber)).
                thenThrow(new CommonBackendException("Order with number 'ORD-00001' not found", HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> orderItemService.getByOrderForWeb(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order with number '%s' not found", orderNumber);

        verifyNoInteractions(orderItemMapper);
        verifyNoInteractions(orderItemRepository);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
    }

    @Test
    void shouldUpdateOrderItem() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Order order = OrderTestData.entityBuilder().id(1L).build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        OrderItemRequest request = OrderItemTestData.request();
        OrderItemResponse response = OrderItemTestData.response();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);
        when(orderItemRepository.save(entity)).thenReturn(entity);
        when(orderItemMapper.toResponseDto(entity)).thenReturn(response);

        OrderItemResponse result = orderItemService.update(orderNumber, id, request);

        assertThat(result).isEqualTo(response);
        verify(orderItemRepository).findById(id);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(orderItemRepository).save(entity);
        verify(orderItemMapper).updateEntityFromDto(request, entity);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldThrowWhenOrderItemNotFoundOnUpdate() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        OrderItemRequest request = OrderItemTestData.request();

        when(orderItemRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.update(orderNumber, id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order item with id: %s not found", id);

        verify(orderItemRepository).findById(id);
        verify(orderItemRepository, never()).save(any());
        verifyNoInteractions(orderQueryService);
        verifyNoInteractions(stockService);
        verifyNoInteractions(orderItemMapper);
    }

    @Test
    void shouldThrowWhenOrderItemNotBelongToOrderOnUpdate() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Order order = OrderTestData.entityBuilder().id(1L).orderNumber("ORD-00002").build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).order(order).build();
        OrderItemRequest request = OrderItemTestData.request();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);

        assertThatThrownBy(() -> orderItemService.update(orderNumber, id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order item %d belongs to order '%s', not '%s'",
                        entity.getId(), entity.getOrder().getOrderNumber(), orderNumber);

        verify(orderItemRepository, never()).save(entity);
        verify(orderItemRepository).findById(id);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemMapper);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldThrowWhenOrderStatusIsNotNewOnUpdate() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        OrderItemRequest request = OrderItemTestData.request();
        Order order = OrderTestData.entityBuilder().id(1L).type(OrderType.OUTBOUND).status(OrderStatus.COMPLETED).build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).order(order).build();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);

        assertThatThrownBy(() -> orderItemService.update(orderNumber, id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot modify order with status: %s", order.getStatus());

        verify(orderItemRepository).findById(id);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(orderItemRepository, never()).save(entity);
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemMapper);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldChangeProductAndQuantityOnUpdateOrderItem() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Order order = OrderTestData.entityBuilder().id(1L).type(OrderType.OUTBOUND).build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        OrderItemRequest request = OrderItemTestData.request("BRP-ENG-002", 20);
        OrderItemResponse response = OrderItemTestData.response();
        Product product = ProductTestData.entityBuilder().id(2L).sku("BRP-ENG-002").build();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);
        when(productService.getBySku(product.getSku())).thenReturn(product);
        when(orderItemRepository.existsByOrderIdAndProductId(order.getId(), product.getId())).thenReturn(false);
        when(orderItemRepository.save(entity)).thenReturn(entity);
        when(orderItemMapper.toResponseDto(entity)).thenReturn(response);

        OrderItemResponse result = orderItemService.update(orderNumber, id, request);

        assertThat(result).isEqualTo(response);
        verify(orderItemRepository).findById(id);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(productService).getBySku(request.productSku());
        verify(orderItemRepository).existsByOrderIdAndProductId(order.getId(), product.getId());
        verify(stockService).checkAvailability(entity.getProduct().getSku(), entity.getQuantity());
        verify(orderItemMapper).updateEntityFromDto(request, entity);
        verify(orderItemRepository).save(entity);
    }

    @Test
    void shouldNotChangeProductAndQuantityWhenBothAreNull() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Order order = OrderTestData.entityBuilder().id(1L).type(OrderType.OUTBOUND).build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        OrderItemRequest request = OrderItemTestData.request(null, null);
        OrderItemResponse response = OrderItemTestData.response();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);
        when(orderItemRepository.save(entity)).thenReturn(entity);
        when(orderItemMapper.toResponseDto(entity)).thenReturn(response);

        OrderItemResponse result = orderItemService.update(orderNumber, id, request);

        assertThat(result).isEqualTo(response);
        verify(orderItemRepository).findById(id);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(orderItemMapper).updateEntityFromDto(request, entity);
        verify(orderItemRepository).save(entity);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldChangeProductOnUpdateOrderItem() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Order order = OrderTestData.entityBuilder().id(1L).build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        OrderItemRequest request = OrderItemTestData.request("BRP-ENG-002", 10);
        OrderItemResponse response = OrderItemTestData.response();
        Product product = ProductTestData.entityBuilder().id(2L).sku("BRP-ENG-002").build();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);
        when(productService.getBySku(product.getSku())).thenReturn(product);
        when(orderItemRepository.existsByOrderIdAndProductId(order.getId(), product.getId())).thenReturn(false);
        when(orderItemRepository.save(entity)).thenReturn(entity);
        when(orderItemMapper.toResponseDto(entity)).thenReturn(response);

        OrderItemResponse result = orderItemService.update(orderNumber, id, request);

        assertThat(result).isEqualTo(response);
        verify(orderItemRepository).findById(id);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(productService).getBySku(request.productSku());
        verify(orderItemRepository).existsByOrderIdAndProductId(order.getId(), product.getId());
        verify(orderItemMapper).updateEntityFromDto(request, entity);
        verify(orderItemRepository).save(entity);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldChangeQuantityOnUpdateOrderItem() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Order order = OrderTestData.entityBuilder().id(1L).build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        OrderItemRequest request = OrderItemTestData.request("BRP-ENG-001", 20);
        OrderItemResponse response = OrderItemTestData.response();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);
        when(orderItemRepository.save(entity)).thenReturn(entity);
        when(orderItemMapper.toResponseDto(entity)).thenReturn(response);

        OrderItemResponse result = orderItemService.update(orderNumber, id, request);

        assertThat(result).isEqualTo(response);
        verify(orderItemRepository).findById(id);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(orderItemMapper).updateEntityFromDto(request, entity);
        verify(orderItemRepository).save(entity);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldChangeItemQuantityWhenOrderOutboundOnUpdateOrderItem() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Order order = OrderTestData.entityBuilder().id(1L).type(OrderType.OUTBOUND).build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        OrderItemRequest request = OrderItemTestData.request("BRP-ENG-001", 20);
        OrderItemResponse response = OrderItemTestData.response();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);
        when(orderItemRepository.save(entity)).thenReturn(entity);
        when(orderItemMapper.toResponseDto(entity)).thenReturn(response);

        OrderItemResponse result = orderItemService.update(orderNumber, id, request);

        assertThat(result).isEqualTo(response);
        verify(orderItemRepository).findById(id);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(orderItemMapper).updateEntityFromDto(request, entity);
        verify(orderItemRepository).save(entity);
        verify(stockService).checkAvailability(entity.getProduct().getSku(), entity.getQuantity());
    }

    @Test
    void shouldThrowWhenQuantityIsZeroOrNegativeOnUpdateOrderItem() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Order order = OrderTestData.entityBuilder().id(1L).build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        OrderItemRequest request = OrderItemTestData.request("BRP-ENG-001", 0);

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);

        assertThatThrownBy(() -> orderItemService.update(orderNumber, id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Quantity must be positive");

        verify(orderItemRepository).findById(id);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(orderItemRepository, never()).save(entity);
        verifyNoInteractions(orderItemMapper);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldThrowWhenProductNotFoundOnUpdateOrderItem() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Order order = OrderTestData.entityBuilder().id(1L).build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        OrderItemRequest request = OrderItemTestData.request("BRP-ENG-002", 10);

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);
        when(productService.getBySku(request.productSku())).thenThrow(new CommonBackendException
                (String.format("Product with SKU '%s' not found", request.productSku()), HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> orderItemService.update(orderNumber, id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Product with SKU '%s' not found", request.productSku());

        verify(orderItemRepository).findById(id);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(productService).getBySku(request.productSku());
        verify(orderItemRepository, never()).save(entity);
        verifyNoInteractions(orderItemMapper);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldThrowWhenProductAlreadyInOrderOnUpdateOrderItem() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Product product = ProductTestData.entityBuilder().id(2L).sku("BRP-ENG-002").build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();
        Order order = OrderTestData.entityBuilder().id(1L).build();
        OrderItemRequest request = OrderItemTestData.request("BRP-ENG-002", 10);

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderQueryService.getOrderEntityByNumber(orderNumber)).thenReturn(order);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(orderItemRepository.existsByOrderIdAndProductId(order.getId(), product.getId())).thenReturn(true);

        assertThatThrownBy(() -> orderItemService.update(orderNumber, id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Product already exists in this order");

        verify(orderItemRepository).findById(id);
        verify(orderQueryService).getOrderEntityByNumber(orderNumber);
        verify(productService).getBySku(request.productSku());
        verify(orderItemRepository).existsByOrderIdAndProductId(order.getId(), product.getId());
        verify(orderItemRepository, never()).save(entity);
        verifyNoInteractions(orderItemMapper);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldDeleteOrderItem() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));

        orderItemService.delete(orderNumber, id);

        verify(orderItemRepository).findById(id);
        verify(orderItemRepository).delete(entity);
    }

    @Test
    void shouldThrowWhenOrderItemNotFoundOnDelete() {
        String orderNumber = "ORD-00001";
        Long id = 1L;

        when(orderItemRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.delete(orderNumber, id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order item with id: %s not found", id);

        verify(orderItemRepository).findById(id);
        verify(orderItemRepository, never()).delete(any());
    }

    @Test
    void shouldThrowWhenOrderItemNotBelongToOrderOnDelete() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Order order = OrderTestData.entityBuilder().id(1L).orderNumber("ORD-00002").build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).order(order).build();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderItemService.delete(orderNumber, id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order item %d belongs to order '%s', not '%s'",
                        entity.getId(), entity.getOrder().getOrderNumber(), orderNumber);

        verify(orderItemRepository).findById(id);
        verify(orderItemRepository, never()).delete(any());
    }

    @Test
    void shouldThrowWhenOrderIsNotNewOnDelete() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Order order = OrderTestData.entityBuilder().id(1L).status(OrderStatus.COMPLETED).build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).order(order).build();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderItemService.delete(orderNumber, id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot modify order with status: %s", order.getStatus());

        verify(orderItemRepository).findById(id);
        verify(orderItemRepository, never()).delete(entity);
    }

    @Test
    void shouldAddCollectedQuantity() {
        Order order = OrderTestData.entityBuilder().id(1L).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        OrderItem orderItem = OrderItemTestData.entityBuilder().id(1L).quantity(20).collectedQuantity(5).build();
        OrderItemResponse response = OrderItemTestData.response(20, 20);
        int quantityToAdd = 15;

        when(orderItemRepository.findByOrderAndProduct(order, product)).thenReturn(Optional.of(orderItem));
        when(orderItemRepository.findById(orderItem.getId())).thenReturn(Optional.of(orderItem));
        when(orderItemRepository.save(orderItem)).thenReturn(orderItem);
        when(orderItemMapper.toResponseDto(orderItem)).thenReturn(response);

        orderItemService.addCollectedQuantity(order, product, quantityToAdd);

        verify(orderItemRepository).findByOrderAndProduct(order, product);
        verify(orderItemRepository).findById(orderItem.getId());
        verify(orderItemRepository).save(orderItem);
        verify(eventPublisher).publishEvent(any(OrderItemCompletedEvent.class));
        verify(orderItemMapper).toResponseDto(orderItem);
    }

    @Test
    void shouldThrowWhenOrderItemNotFoundByOrderAndProductOnAddCollectedQuantity() {
        Order order = OrderTestData.entityBuilder().id(1L).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        int quantityToAdd = 15;

        when(orderItemRepository.findByOrderAndProduct(order, product)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.addCollectedQuantity(order, product, quantityToAdd))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("OrderItem not found for order " + order.getOrderNumber() +
                                      " and product " + product.getSku());

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(orderItemMapper);
        verify(orderItemRepository, never()).save(any());
        verify(orderItemRepository).findByOrderAndProduct(order, product);
    }

    @Test
    void shouldThrowWhenOrderItemNotFoundByOrderItemIdOnAddCollectedQuantity() {
        Order order = OrderTestData.entityBuilder().id(1L).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        OrderItem orderItem = OrderItemTestData.entityBuilder().id(1L).build();
        int quantityToAdd = 15;

        when(orderItemRepository.findByOrderAndProduct(order, product)).thenReturn(Optional.of(orderItem));
        when(orderItemRepository.findById(orderItem.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.addCollectedQuantity(order, product, quantityToAdd))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order item with id: %s not found", orderItem.getId());

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(orderItemMapper);
        verify(orderItemRepository, never()).save(any());
        verify(orderItemRepository).findByOrderAndProduct(order, product);
        verify(orderItemRepository).findById(orderItem.getId());
    }

    @Test
    void shouldReturnRemainingQuantity() {
        Order order = OrderTestData.entityBuilder().id(1L).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        OrderItem orderItem = OrderItemTestData.entityBuilder().id(1L).collectedQuantity(7).quantity(15).build();

        when(orderItemRepository.findByOrderAndProduct(order, product)).thenReturn(Optional.of(orderItem));

        int result = orderItemService.getRemainingQuantity(order, product);

        assertThat(result).isEqualTo(8);
        verify(orderItemRepository).findByOrderAndProduct(order, product);
    }

    @Test
    void shouldThrowWhenOrderItemNotFoundByOrderAndProductOnGetRemainingQuantity() {
        Order order = OrderTestData.entityBuilder().id(1L).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();

        when(orderItemRepository.findByOrderAndProduct(order, product)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.getRemainingQuantity(order, product))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("OrderItem not found");

        verify(orderItemRepository).findByOrderAndProduct(order, product);
    }

    @Test
    void shouldSetCollectedQuantity() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Integer collectedQuantity = 20;
        OrderItem orderItem = OrderItemTestData.entityBuilder().id(1L).quantity(20).collectedQuantity(5).build();
        OrderItemResponse response = OrderItemTestData.response(20, 20);

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(orderItem));
        when(orderItemRepository.save(orderItem)).thenReturn(orderItem);
        when(orderItemMapper.toResponseDto(orderItem)).thenReturn(response);

        OrderItemResponse result = orderItemService.setCollectedQuantity(orderNumber, id, collectedQuantity);

        assertThat(result).isEqualTo(response);
        verify(orderItemRepository).findById(id);
        verify(orderItemRepository).save(orderItem);
        verify(eventPublisher).publishEvent(any(OrderItemCompletedEvent.class));
        verify(orderItemMapper).toResponseDto(orderItem);
    }

    @Test
    void shouldSetCollectedQuantityWhenNotFullyCollected() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Integer collectedQuantity = 15;
        OrderItem orderItem = OrderItemTestData.entityBuilder().id(1L).quantity(20).collectedQuantity(5).build();
        OrderItemResponse response = OrderItemTestData.response(20, 15);

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(orderItem));
        when(orderItemRepository.save(orderItem)).thenReturn(orderItem);
        when(orderItemMapper.toResponseDto(orderItem)).thenReturn(response);

        OrderItemResponse result = orderItemService.setCollectedQuantity(orderNumber, id, collectedQuantity);

        assertThat(result).isEqualTo(response);
        verify(orderItemRepository).findById(id);
        verify(orderItemRepository).save(orderItem);
        verify(eventPublisher, never()).publishEvent(any(OrderItemCompletedEvent.class));
        verify(orderItemMapper).toResponseDto(orderItem);
    }

    @Test
    void shouldThrowWhenOrderItemNotFoundByIdOnSetCollectedQuantity() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Integer collectedQuantity = 5;

        when(orderItemRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.setCollectedQuantity(orderNumber, id, collectedQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order item with id: %s not found", id);

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(orderItemMapper);
        verify(orderItemRepository, never()).save(any());
        verify(orderItemRepository).findById(id);
    }

    @Test
    void shouldThrowWhenOrderItemNotBelongToOrderOnSetCollectedQuantity() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Integer collectedQuantity = 5;
        Order order = OrderTestData.entityBuilder().id(1L).orderNumber("ORD-00002").build();
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).order(order).build();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderItemService.setCollectedQuantity(orderNumber, id, collectedQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order item %d belongs to order '%s', not '%s'",
                        id, entity.getOrder().getOrderNumber(), orderNumber);

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(orderItemMapper);
        verify(orderItemRepository, never()).save(any());
        verify(orderItemRepository).findById(id);
    }

    @Test
    void shouldThrowWhenCollectedQuantityNegativeOnSetCollectedQuantity() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Integer collectedQuantity = -5;
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderItemService.setCollectedQuantity(orderNumber, id, collectedQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Collected quantity cannot be negative");

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(orderItemMapper);
        verify(orderItemRepository, never()).save(any());
        verify(orderItemRepository).findById(id);
    }

    @Test
    void shouldThrowWhenCollectedQuantityGreaterThanExpectedOnSetCollectedQuantity() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        Integer collectedQuantity = 20;
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderItemService.setCollectedQuantity(orderNumber, id, collectedQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Collected quantity cannot exceed ordered quantity");

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(orderItemMapper);
        verify(orderItemRepository, never()).save(any());
        verify(orderItemRepository).findById(id);
    }

    @Test
    void shouldThrowWhenCollectedQuantityIsNullOnSetCollectedQuantity() {
        String orderNumber = "ORD-00001";
        Long id = 1L;
        OrderItem entity = OrderItemTestData.entityBuilder().id(1L).build();

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> orderItemService.setCollectedQuantity(orderNumber, id, null))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Collected quantity must not be null");

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(orderItemMapper);
        verify(orderItemRepository, never()).save(any());
        verify(orderItemRepository).findById(id);
    }
}