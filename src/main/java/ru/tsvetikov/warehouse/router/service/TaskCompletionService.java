package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.WarehouseTask;
import ru.tsvetikov.warehouse.router.model.db.repository.WarehouseTaskRepository;


@Slf4j
@Service
@RequiredArgsConstructor
public class TaskCompletionService {

    private final WarehouseTaskRepository warehouseTaskRepository;
    private final OrderItemService orderItemService;

    @Transactional  // можно оставить
    public void finalizeTask(Long taskId, Integer confirmedQuantity) {
        WarehouseTask task = warehouseTaskRepository.findById(taskId)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Task with id: %s not found", taskId), HttpStatus.NOT_FOUND));

        if (task.getOrder() != null && task.getProduct() != null) {
            orderItemService.addCollectedQuantity(
                    task.getOrder(),
                    task.getProduct(),
                    confirmedQuantity
            );
        }

        log.info("Task {} finalized - OrderItem updated with quantity {}", taskId, confirmedQuantity);
    }
}