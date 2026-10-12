package ru.tsvetikov.warehouse.router.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Order;
import ru.tsvetikov.warehouse.router.model.db.repository.OrderRepository;
import ru.tsvetikov.warehouse.router.testdata.OrderTestData;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderQueryService orderQueryService;

    @Test
    void shouldFindOrderEntityByNumber() {
        String orderNumber = "ORD-00001";
        Order entity = OrderTestData.entityBuilder().id(1L).build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(entity));

        Order result = orderQueryService.getOrderEntityByNumber(orderNumber);

        assertThat(result).isEqualTo(entity);
        verify(orderRepository).findByOrderNumber("ORD-00001");
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        String orderNumber = "ORD-00999";

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderQueryService.getOrderEntityByNumber(orderNumber))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Order with number '%s' not found", orderNumber);

        verify(orderRepository).findByOrderNumber(orderNumber);
    }
}