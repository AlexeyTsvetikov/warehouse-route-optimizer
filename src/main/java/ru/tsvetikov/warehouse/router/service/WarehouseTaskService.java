//package ru.tsvetikov.warehouse.router.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
//import ru.tsvetikov.warehouse.router.model.db.entity.*;
//import ru.tsvetikov.warehouse.router.model.db.repository.*;
//import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;
//import ru.tsvetikov.warehouse.router.model.dto.response.WarehouseTaskResponse;
//import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskStatus;
//import ru.tsvetikov.warehouse.router.model.mapper.WarehouseTaskMapper;
//import ru.tsvetikov.warehouse.router.service.utill.EntityFinder;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;

//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class WarehouseTaskService {
//    private final WarehouseTaskRepository warehouseTaskRepository;
//    private final WarehouseTaskMapper warehouseTaskMapper;
//    private final EntityFinder entityFinder;
//
//    @Transactional
//    public WarehouseTaskResponse create(WarehouseTaskRequest request) {
//        WarehouseTask warehouseTask = new WarehouseTask();
//        warehouseTask.setType(request.type());
//        warehouseTask.setStatus(WarehouseTaskStatus.CREATED);
//        warehouseTask.setTaskNumber(generateTaskNumber());
//
//        warehouseTask.setProduct(entityFinder.findProductByTrackingNumber(request.productTrackingNumber()));
//        warehouseTask.setSourceLocation(entityFinder.findCellByCode(request.sourceCellCode()));
//        warehouseTask.setTargetLocation(entityFinder.findCellByCode(request.targetCellCode()));
//
//        warehouseTask.setAssignedUser(entityFinder.findUserByUsername(request.username()));
//        warehouseTask.setOrder(entityFinder.findOrderByNumber(request.orderNumber()));
//
//        WarehouseTask saved = warehouseTaskRepository.save(warehouseTask);
//        return warehouseTaskMapper.toResponseDto(saved);
//    }
//
//    @Transactional(readOnly = true)
//    public WarehouseTaskResponse getById(Long id) {
//        WarehouseTask warehouseTask = getTaskEntityById(id);
//        return warehouseTaskMapper.toResponseDto(warehouseTask);
//    }
//
//    @Transactional(readOnly = true)
//    public List<WarehouseTaskResponse> getAll() {
//        return warehouseTaskRepository.findAll().stream()
//                .map(warehouseTaskMapper::toResponseDto)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional
//    public WarehouseTaskResponse assignTask(Long taskId, String username) {
//        WarehouseTask warehouseTask = getTaskEntityById(taskId);
//
//        if (warehouseTask.getStatus() != WarehouseTaskStatus.CREATED) {
//            throw new CommonBackendException(
//                    "Cannot assign warehouseTask with status: " + warehouseTask.getStatus(),
//                    HttpStatus.BAD_REQUEST);
//        }
//
//        User user = entityFinder.findUserByUsername(username);
//        warehouseTask.setAssignedUser(user);
//        warehouseTask.setStatus(WarehouseTaskStatus.ASSIGNED);
//        warehouseTask.setUpdatedAt(LocalDateTime.now());
//
//        return warehouseTaskMapper.toResponseDto(warehouseTaskRepository.save(warehouseTask));
//    }
//
//    @Transactional
//    public WarehouseTaskResponse startTask(Long taskId) {
//        WarehouseTask warehouseTask = getTaskEntityById(taskId);
//
//        warehouseTask.setStatus(WarehouseTaskStatus.IN_PROGRESS);
//        warehouseTask.setUpdatedAt(LocalDateTime.now());
//
//        return warehouseTaskMapper.toResponseDto(warehouseTaskRepository.save(warehouseTask));
//    }
//
//    @Transactional
//    public WarehouseTaskResponse completeTask(Long taskId) {
//        WarehouseTask warehouseTask = getTaskEntityById(taskId);
//
//        warehouseTask.setStatus(WarehouseTaskStatus.COMPLETED);
//        warehouseTask.setCompletedAt(LocalDateTime.now());
//
//        // Позже добавить:
//        // updateRelatedEntitiesOnCompletion(warehouseTask);
//        return warehouseTaskMapper.toResponseDto(warehouseTaskRepository.save(warehouseTask));
//    }
//
//    @Transactional
//    public WarehouseTaskResponse update(Long id, WarehouseTaskRequest request) {
//        WarehouseTask existing = getTaskEntityById(id);
//
//        if (existing.getStatus() != WarehouseTaskStatus.CREATED) {
//            throw new CommonBackendException(
//                    "Cannot update task with status: " + existing.getStatus(),
//                    HttpStatus.BAD_REQUEST);
//        }
//
//        if (request.type() != null) {
//            existing.setType(request.type());
//        }
//
//        if (request.productTrackingNumber() != null) {
//            existing.setProduct(entityFinder.findProductByTrackingNumber(request.productTrackingNumber()));
//        }
//        if (request.sourceCellCode() != null) {
//            existing.setSourceLocation(entityFinder.findCellByCode(request.sourceCellCode()));
//        }
//        if (request.targetCellCode() != null) {
//            existing.setTargetLocation(entityFinder.findCellByCode(request.targetCellCode()));
//        }
//        if (request.username() != null) {
//            existing.setAssignedUser(entityFinder.findUserByUsername(request.username()));
//        }
//        if (request.orderNumber() != null) {
//            existing.setOrder(entityFinder.findOrderByNumber(request.orderNumber()));
//        }
//
//        WarehouseTask updated = warehouseTaskRepository.save(existing);
//        return warehouseTaskMapper.toResponseDto(updated);
//    }
//
//    @Transactional
//    public void delete(Long id) {
//        WarehouseTask warehouseTask = getTaskEntityById(id);
//        warehouseTask.setStatus(WarehouseTaskStatus.CANCELLED);
//    }
//
//    private WarehouseTask getTaskEntityById(Long id) {
//        return warehouseTaskRepository.findById(id)
//                .orElseThrow(() -> new CommonBackendException("WarehouseTask not found with id: " + id, HttpStatus.NOT_FOUND));
//    }
//
//    private String generateTaskNumber() {
//        Long lastId = warehouseTaskRepository.findMaxId().orElse(0L);
//        String newNumber = String.format("TASK-%06d", lastId + 1);
//
//        if (warehouseTaskRepository.existsByTaskNumber(newNumber)) {
//            throw new CommonBackendException("WarehouseTask number conflict: " + newNumber, HttpStatus.CONFLICT);
//        }
//        return newNumber;
//    }
//}
