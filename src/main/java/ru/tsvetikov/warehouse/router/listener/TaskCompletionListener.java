package ru.tsvetikov.warehouse.router.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.tsvetikov.warehouse.router.event.TaskCompletedEvent;
import ru.tsvetikov.warehouse.router.model.db.entity.WarehouseTask;
import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;
import ru.tsvetikov.warehouse.router.service.WarehouseTaskManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCompletionListener {

    private final WarehouseTaskManager warehouseTaskManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCompleted(TaskCompletedEvent event) {
        WarehouseTask task = event.task();

        if (task.getType() == WarehouseTaskType.PICKING
            && task.getConfirmedQuantity() < task.getPlannedQuantity()
            && task.getSourceLocation() != null
            && task.getOrder() != null) {

            int remaining = task.getPlannedQuantity() - task.getConfirmedQuantity();
            log.info("Creating re-picking task for {}: {} items remaining",
                    task.getProduct().getSku(), remaining);

            WarehouseTaskRequest rePickRequest = WarehouseTaskRequest.builder()
                    .type(WarehouseTaskType.PICKING)
                    .productSku(task.getProduct().getSku())
                    .plannedQuantity(remaining)
                    .sourceLocationCode(task.getSourceLocation().getCode())
                    .orderNumber(task.getOrder().getOrderNumber())
                    .build();

            warehouseTaskManager.createSingleTask(rePickRequest);
        }
    }
}
