package ru.tsvetikov.warehouse.router.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.WarehouseTask;
import ru.tsvetikov.warehouse.router.model.db.repository.WarehouseTaskRepository;
import ru.tsvetikov.warehouse.router.testdata.WarehouseTaskTestData;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskCompletionServiceTest {

    @Mock
    private WarehouseTaskRepository warehouseTaskRepository;

    @Mock
    private OrderItemService orderItemService;

    @InjectMocks
    private TaskCompletionService taskCompletionService;

    @Test
    void shouldFinalizeTaskAndUpdateOrderItem() {
        Long taskId = 1L;
        int confirmedQuantity = 10;
        WarehouseTask entity = WarehouseTaskTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findById(taskId)).thenReturn(Optional.of(entity));

        taskCompletionService.finalizeTask(taskId, confirmedQuantity);

        verify(warehouseTaskRepository).findById(taskId);
        verify(orderItemService).addCollectedQuantity(entity.getOrder(), entity.getProduct(), confirmedQuantity);

    }

    @Test
    void shouldNotUpdateOrderItemWhenOrderIsNull() {
        Long taskId = 1L;
        int confirmedQuantity = 10;
        WarehouseTask task = WarehouseTaskTestData.entityBuilder().id(1L).order(null).build();

        when(warehouseTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        taskCompletionService.finalizeTask(taskId, confirmedQuantity);

        verify(orderItemService, never()).addCollectedQuantity(any(), any(), anyInt());
    }

    @Test
    void shouldNotUpdateOrderItemWhenProductIsNull() {
        Long taskId = 1L;
        int confirmedQuantity = 10;
        WarehouseTask task = WarehouseTaskTestData.entityBuilder().id(1L).product(null).build();

        when(warehouseTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        taskCompletionService.finalizeTask(taskId, confirmedQuantity);

        verify(orderItemService, never()).addCollectedQuantity(any(), any(), anyInt());
    }

    @Test
    void shouldThrowWhenTaskNotFound() {
        Long id = 99L;

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskCompletionService.finalizeTask(id, 10))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Task with id: %s not found", id);

        verifyNoInteractions(orderItemService);
    }
}