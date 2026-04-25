package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.WarehouseTask;
import ru.tsvetikov.warehouse.router.model.db.repository.WarehouseTaskRepository;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskStatus;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskCompletionService {

    private final WarehouseTaskRepository warehouseTaskRepository;

    @Transactional
    public void finalizeTask(Long taskId, Integer confirmedQuantity) {
        WarehouseTask task = warehouseTaskRepository.findById(taskId)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Task with id: %s not found", taskId), HttpStatus.NOT_FOUND));

        if (task.getStatus() != WarehouseTaskStatus.IN_PROGRESS) {
            log.warn("Task {} is not IN_PROGRESS, current status: {}", taskId, task.getStatus());
            return;
        }

        task.setStatus(WarehouseTaskStatus.COMPLETED);
        task.setConfirmedQuantity(confirmedQuantity);
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        warehouseTaskRepository.save(task);
        log.info("Task {} completed with quantity {}", taskId, confirmedQuantity);
    }
}
