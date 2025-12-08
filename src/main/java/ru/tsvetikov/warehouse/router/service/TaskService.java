package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.*;
import ru.tsvetikov.warehouse.router.model.db.repository.*;
import ru.tsvetikov.warehouse.router.model.dto.request.TaskRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.TaskResponse;
import ru.tsvetikov.warehouse.router.model.enums.TaskStatus;
import ru.tsvetikov.warehouse.router.model.mapper.TaskMapper;
import ru.tsvetikov.warehouse.router.service.utill.EntityFinder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final EntityFinder entityFinder;

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        task.setType(request.type());
        task.setStatus(TaskStatus.CREATED);
        task.setTaskNumber(generateTaskNumber());

        task.setProduct(entityFinder.findProductByTrackingNumber(request.productTrackingNumber()));
        task.setSourceCell(entityFinder.findCellByCode(request.sourceCellCode()));
        task.setTargetCell(entityFinder.findCellByCode(request.targetCellCode()));

        task.setUser(entityFinder.findUserByUsername(request.username()));
        task.setOrder(entityFinder.findOrderByNumber(request.orderNumber()));

        Task saved = taskRepository.save(task);
        return taskMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(Long id) {
        Task task = getTaskEntityById(id);
        return taskMapper.toResponseDto(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAll() {
        return taskRepository.findAll().stream()
                .map(taskMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse assignTask(Long taskId, String username) {
        Task task = getTaskEntityById(taskId);

        if (task.getStatus() != TaskStatus.CREATED) {
            throw new CommonBackendException(
                    "Cannot assign task with status: " + task.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        User user = entityFinder.findUserByUsername(username);
        task.setUser(user);
        task.setStatus(TaskStatus.ASSIGNED);
        task.setAssignedAt(LocalDateTime.now());

        return taskMapper.toResponseDto(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse startTask(Long taskId) {
        Task task = getTaskEntityById(taskId);

        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStartedAt(LocalDateTime.now());

        return taskMapper.toResponseDto(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse completeTask(Long taskId) {
        Task task = getTaskEntityById(taskId);

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        // Позже добавить:
        // updateRelatedEntitiesOnCompletion(task);
        return taskMapper.toResponseDto(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task existing = getTaskEntityById(id);

        if (existing.getStatus() != TaskStatus.CREATED) {
            throw new CommonBackendException(
                    "Cannot update task with status: " + existing.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        if (request.type() != null) {
            existing.setType(request.type());
        }

        if (request.productTrackingNumber() != null) {
            existing.setProduct(entityFinder.findProductByTrackingNumber(request.productTrackingNumber()));
        }
        if (request.sourceCellCode() != null) {
            existing.setSourceCell(entityFinder.findCellByCode(request.sourceCellCode()));
        }
        if (request.targetCellCode() != null) {
            existing.setTargetCell(entityFinder.findCellByCode(request.targetCellCode()));
        }
        if (request.username() != null) {
            existing.setUser(entityFinder.findUserByUsername(request.username()));
        }
        if (request.orderNumber() != null) {
            existing.setOrder(entityFinder.findOrderByNumber(request.orderNumber()));
        }

        Task updated = taskRepository.save(existing);
        return taskMapper.toResponseDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        Task task = getTaskEntityById(id);
        task.setStatus(TaskStatus.CANCELLED);
    }

    private Task getTaskEntityById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new CommonBackendException("Task not found with id: " + id, HttpStatus.NOT_FOUND));
    }

    private String generateTaskNumber() {
        Long lastId = taskRepository.findMaxId().orElse(0L);
        String newNumber = String.format("TASK-%06d", lastId + 1);

        if (taskRepository.existsByTaskNumber(newNumber)) {
            throw new CommonBackendException("Task number conflict: " + newNumber, HttpStatus.CONFLICT);
        }
        return newNumber;
    }
}
