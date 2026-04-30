package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.event.TaskCompletedEvent;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.*;
import ru.tsvetikov.warehouse.router.model.db.repository.*;
import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.WarehouseTaskResponse;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskStatus;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;
import ru.tsvetikov.warehouse.router.model.mapper.WarehouseTaskMapper;
import ru.tsvetikov.warehouse.router.utils.PaginationUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseTaskService {
    private final WarehouseTaskRepository warehouseTaskRepository;
    private final WarehouseTaskMapper warehouseTaskMapper;
    private final TaskCompletionService taskCompletionService;
    private final StockService stockService;
    private final ProductService productService;
    private final LocationService locationService;
    private final UserService userService;
    private final OrderQueryService orderQueryService;
    private final OrderItemService orderItemService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public WarehouseTaskResponse create(WarehouseTaskRequest request) {
        String taskNumber = generateTaskNumber();

        // Защита от коллизий (на случай параллельных запросов)
        int attempts = 0;
        while (warehouseTaskRepository.existsByTaskNumber(taskNumber) && attempts < 3) {
            taskNumber = generateTaskNumber();
            attempts++;
        }

        if (warehouseTaskRepository.existsByTaskNumber(taskNumber)) {
            throw new CommonBackendException("Failed to generate unique task number after 3 attempts",
                    HttpStatus.CONFLICT);
        }

        Product product = findProductBySkuOrThrow(request.productSku());

        Location sourceLocation = null;
        if (request.sourceLocationCode() != null) {
            sourceLocation = findLocationByCodeOrThrow(request.sourceLocationCode());
        }

        Location targetLocation = null;
        if (request.targetLocationCode() != null) {
            targetLocation = findLocationByCodeOrThrow(request.targetLocationCode());
        }

        if (sourceLocation == null && targetLocation == null) {
            throw new CommonBackendException(
                    "At least one location (source or target) must be specified",
                    HttpStatus.BAD_REQUEST);
        }

        if (request.orderNumber() != null && request.type() == WarehouseTaskType.PICKING) {
            stockService.reserveStock(request.productSku(), request.plannedQuantity());
        }

        WarehouseTask task = new WarehouseTask();
        task.setTaskNumber(taskNumber);
        task.setType(request.type());
        task.setStatus(WarehouseTaskStatus.CREATED);
        task.setPlannedQuantity(request.plannedQuantity());
        task.setConfirmedQuantity(request.confirmedQuantity());
        task.setProduct(product);
        task.setSourceLocation(sourceLocation);
        task.setTargetLocation(targetLocation);

        if (request.assignedUsername() != null && !request.assignedUsername().isBlank()) {
            User user = findUserByUsernameOrThrow(request.assignedUsername());
            task.setAssignedUser(user);
        }

        if (request.orderNumber() != null && !request.orderNumber().isBlank()) {
            Order order = findOrderByNumberOrThrow(request.orderNumber());
            task.setOrder(order);
        }

        WarehouseTask saved = warehouseTaskRepository.save(task);
        return warehouseTaskMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public WarehouseTaskResponse getById(Long id) {
        WarehouseTask task = findTaskOrThrow(id);
        return warehouseTaskMapper.toResponseDto(task);
    }

    @Transactional(readOnly = true)
    public Page<WarehouseTaskResponse> getAll(int page, int size, String sort, Sort.Direction order) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(order, sort));
        Page<WarehouseTask> tasks = warehouseTaskRepository.findAll(pageable);
        return tasks.map(warehouseTaskMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<WarehouseTaskResponse> getByStatus(WarehouseTaskStatus status, int page, int size, String sort, Sort.Direction order) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(order, sort));
        Page<WarehouseTask> tasks = warehouseTaskRepository.findByStatus(status, pageable);
        return tasks.map(warehouseTaskMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<WarehouseTaskResponse> search(String query, int page, int size, String sort, Sort.Direction order) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(order, sort));
        Page<WarehouseTask> tasks = warehouseTaskRepository.search(query, pageable);
        return tasks.map(warehouseTaskMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public boolean areAllTasksCompletedForOrder(String orderNumber, WarehouseTaskType taskType) {
        return warehouseTaskRepository.findByOrderNumberAndType(orderNumber, taskType)
                .stream()
                .allMatch(task -> task.getStatus() == WarehouseTaskStatus.COMPLETED);
    }

    @Transactional
    public WarehouseTaskResponse update(Long id, WarehouseTaskRequest request) {
        WarehouseTask task = findTaskOrThrow(id);

        if (task.getStatus() != WarehouseTaskStatus.CREATED) {
            throw new CommonBackendException(
                    String.format("Cannot update task with status: %s", task.getStatus()), HttpStatus.BAD_REQUEST);
        }

        if (task.getAssignedUser() != null) {
            throw new CommonBackendException(
                    "Cannot update task that is already assigned to user", HttpStatus.BAD_REQUEST);
        }

        if (request.type() != null) {
            task.setType(request.type());
        }

        if (request.plannedQuantity() != null) {
            if (request.plannedQuantity() <= 0) {
                throw new CommonBackendException("Quantity must be positive", HttpStatus.BAD_REQUEST);
            }
            if (task.getOrder() != null && task.getProduct() != null) {
                int remaining = orderItemService.getRemainingQuantity(task.getOrder(), task.getProduct());
                if (request.plannedQuantity() > remaining) {
                    throw new CommonBackendException(
                            String.format("Cannot exceed remaining order quantity. Remaining: %d", remaining),
                            HttpStatus.BAD_REQUEST);
                }
            }
            task.setPlannedQuantity(request.plannedQuantity());
        }

        if (request.confirmedQuantity() != null) {
            task.setConfirmedQuantity(request.confirmedQuantity());
        }

        if (request.productSku() != null && !request.productSku().equals(task.getProduct().getSku())) {
            if (task.getOrder() != null) {
                throw new CommonBackendException(
                        "Cannot change product for task linked to order. Cancel this task and create a new one.",
                        HttpStatus.BAD_REQUEST);
            }
            Product product = findProductBySkuOrThrow(request.productSku());
            task.setProduct(product);
        }

        if (request.sourceLocationCode() != null && !request.sourceLocationCode().isBlank()) {
            if (task.getSourceLocation() == null ||
                !request.sourceLocationCode().equals(task.getSourceLocation().getCode())) {
                Location location = findLocationByCodeOrThrow(request.sourceLocationCode());
                task.setSourceLocation(location);
            }
        }

        if (request.targetLocationCode() != null && !request.targetLocationCode().isBlank()) {
            if (task.getTargetLocation() == null ||
                !request.targetLocationCode().equals(task.getTargetLocation().getCode())) {
                Location location = findLocationByCodeOrThrow(request.targetLocationCode());
                task.setTargetLocation(location);
            }
        }

        if (request.orderNumber() != null) {
            if (task.getOrder() != null &&
                !request.orderNumber().equals(task.getOrder().getOrderNumber())) {
                throw new CommonBackendException("Cannot change order for existing task", HttpStatus.BAD_REQUEST);
            }
            if (task.getOrder() == null) {
                Order order = findOrderByNumberOrThrow(request.orderNumber());
                task.setOrder(order);
            }
        } else {
            task.setOrder(null);
        }

        task.setUpdatedAt(LocalDateTime.now());
        WarehouseTask updated = warehouseTaskRepository.save(task);
        return warehouseTaskMapper.toResponseDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        WarehouseTask task = findTaskOrThrow(id);

        if (task.getStatus() != WarehouseTaskStatus.CREATED &&
            task.getStatus() != WarehouseTaskStatus.ASSIGNED &&
            task.getStatus() != WarehouseTaskStatus.IN_PROGRESS) {
            throw new CommonBackendException(
                    String.format("Cannot cancel task with status: %s", task.getStatus()),
                    HttpStatus.BAD_REQUEST);
        }

        if (task.getType() == WarehouseTaskType.PICKING) {
            Integer confirmed = task.getConfirmedQuantity() != null ? task.getConfirmedQuantity() : 0;
            int reservedQuantity = task.getPlannedQuantity() - confirmed;
            if (reservedQuantity > 0) {
                stockService.releaseReserved(task.getProduct().getSku(), reservedQuantity);
                log.info("Released reserved stock for canceled PICKING task: product {}, quantity {}",
                        task.getProduct().getSku(), reservedQuantity);
            }
        }

        task.setStatus(WarehouseTaskStatus.CANCELLED);
        task.setUpdatedAt(LocalDateTime.now());
        warehouseTaskRepository.save(task);
    }

    @Transactional
    public WarehouseTaskResponse assignTask(Long id, String username) {
        WarehouseTask task = findTaskOrThrow(id);

        if (task.getStatus() != WarehouseTaskStatus.CREATED) {
            throw new CommonBackendException(
                    String.format("Cannot assign task with status: %s", task.getStatus()), HttpStatus.BAD_REQUEST);
        }

        User user = findUserByUsernameOrThrow(username);
        task.setAssignedUser(user);
        task.setStatus(WarehouseTaskStatus.ASSIGNED);
        task.setUpdatedAt(LocalDateTime.now());

        WarehouseTask saved = warehouseTaskRepository.save(task);
        return warehouseTaskMapper.toResponseDto(saved);
    }

    @Transactional
    public WarehouseTaskResponse startTask(Long id) {
        WarehouseTask task = findTaskOrThrow(id);

        if (task.getStatus() != WarehouseTaskStatus.ASSIGNED) {
            throw new CommonBackendException(
                    String.format("Cannot start task with status: %s", task.getStatus()), HttpStatus.BAD_REQUEST);
        }

        task.setStatus(WarehouseTaskStatus.IN_PROGRESS);
        task.setUpdatedAt(LocalDateTime.now());

        WarehouseTask saved = warehouseTaskRepository.save(task);
        return warehouseTaskMapper.toResponseDto(saved);
    }

    @Transactional
    public WarehouseTaskResponse completeTask(Long id, Integer confirmedQuantity) {
        WarehouseTask task = findTaskOrThrow(id);

        if (task.getStatus() != WarehouseTaskStatus.IN_PROGRESS) {
            throw new CommonBackendException(
                    String.format("Cannot complete task with status: %s", task.getStatus()),
                    HttpStatus.BAD_REQUEST);
        }

        int finalQuantity = confirmedQuantity != null ? confirmedQuantity : task.getPlannedQuantity();

        if (finalQuantity < 0) {
            throw new CommonBackendException("Confirmed quantity cannot be negative", HttpStatus.BAD_REQUEST);
        }

        task.setStatus(WarehouseTaskStatus.COMPLETED);
        task.setConfirmedQuantity(finalQuantity);
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        updateStockForTask(task, finalQuantity);

        WarehouseTask saved = warehouseTaskRepository.save(task);

        // Завершаем OrderItem
        taskCompletionService.finalizeTask(saved.getId(), finalQuantity);
        eventPublisher.publishEvent(new TaskCompletedEvent(saved));


        return warehouseTaskMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<WarehouseTaskResponse> getTasksByUserAndStatus(String username, List<WarehouseTaskStatus> statuses,
                                                               Integer page, Integer perPage, String sort,
                                                               Sort.Direction order) {
        User user = findUserByUsernameOrThrow(username);
        Pageable pageRequest = PaginationUtils.getPageRequest(page, perPage, sort, order);

        if (statuses == null || statuses.isEmpty()) {
            statuses = Arrays.asList(
                    WarehouseTaskStatus.CREATED,
                    WarehouseTaskStatus.ASSIGNED,
                    WarehouseTaskStatus.IN_PROGRESS,
                    WarehouseTaskStatus.COMPLETED);
        }

        Page<WarehouseTask> tasks = warehouseTaskRepository.findByAssignedUserIdAndStatusIn(
                user.getId(), statuses, pageRequest);
        return tasks.map(warehouseTaskMapper::toResponseDto);
    }

    private WarehouseTask findTaskOrThrow(Long id) {
        return warehouseTaskRepository.findById(id)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Task with id: %s not found", id), HttpStatus.NOT_FOUND));
    }

    private Product findProductBySkuOrThrow(String sku) {
        return productService.getProductEntityBySku(sku);
    }

    private Location findLocationByCodeOrThrow(String code) {
        return locationService.getLocationEntityByLocationCode(code);
    }

    private User findUserByUsernameOrThrow(String username) {
        return userService.getUserEntityByUserName(username);
    }

    private Order findOrderByNumberOrThrow(String orderNumber) {
        return orderQueryService.getOrderEntityByNumber(orderNumber);
    }

    private String generateTaskNumber() {
        Long maxId = warehouseTaskRepository.findMaxId().orElse(0L);
        return String.format("TASK-%05d", maxId + 1);
    }

    private void updateStockForTask(WarehouseTask task, Integer confirmedQuantity) {
        String productSku = task.getProduct().getSku();

        switch (task.getType()) {
            case PICKING ->
                    stockService.decreaseStock(productSku, task.getSourceLocation().getCode(), confirmedQuantity);
            case MOVEMENT -> {
                stockService.transferStock(productSku, confirmedQuantity,
                        task.getSourceLocation().getCode(),
                        task.getTargetLocation().getCode());
                log.debug("Stock transferred for MOVEMENT task: {} from {} to {}, quantity {}",
                        productSku, task.getSourceLocation().getCode(),
                        task.getTargetLocation().getCode(), confirmedQuantity);
            }
            case RECEIVING -> {
                stockService.increaseStock(productSku, task.getTargetLocation().getCode(), confirmedQuantity);
                log.debug("Stock increased for RECEIVING task: {} @ {} +{}",
                        productSku, task.getTargetLocation().getCode(), confirmedQuantity);
            }
            default -> log.warn("Unsupported task type for stock update: {}", task.getType());
        }
    }
}