package ru.tsvetikov.warehouse.router.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import ru.tsvetikov.warehouse.router.event.TaskCompletedEvent;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.*;
import ru.tsvetikov.warehouse.router.model.db.repository.WarehouseTaskRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.WarehouseTaskResponse;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskStatus;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;
import ru.tsvetikov.warehouse.router.model.mapper.WarehouseTaskMapper;
import ru.tsvetikov.warehouse.router.testdata.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class WarehouseTaskServiceTest {

    @Mock
    private WarehouseTaskRepository warehouseTaskRepository;

    @Mock
    private WarehouseTaskMapper warehouseTaskMapper;

    @Mock
    private TaskCompletionService taskCompletionService;

    @Mock
    private StockService stockService;

    @Mock
    private ProductService productService;

    @Mock
    private LocationService locationService;

    @Mock
    private UserService userService;

    @Mock
    private OrderQueryService orderQueryService;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WarehouseTaskService warehouseTaskService;

    @Test
    void shouldCreateWarehouseTask() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location1 = LocationTestData.entityBuilder().id(1L).build();
        Location location2 = LocationTestData.entityBuilder().id(2L).build();
        User user = UserTestData.entityBuilder().id(1L).build();
        Order order = OrderTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(locationService.getByCode(request.sourceLocationCode())).thenReturn(location1);
        when(locationService.getByCode(request.targetLocationCode())).thenReturn(location2);
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(orderQueryService.getOrderEntityByNumber(request.orderNumber())).thenReturn(order);
        when(warehouseTaskRepository.save(any(WarehouseTask.class))).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(any(WarehouseTask.class))).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.create(request);

        assertThat(result).isEqualTo(response);
        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
        verify(locationService).getByCode(request.sourceLocationCode());
        verify(locationService).getByCode(request.targetLocationCode());
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(orderQueryService).getOrderEntityByNumber(request.orderNumber());
        verify(warehouseTaskRepository).save(any(WarehouseTask.class));
        verify(stockService).reserveStockNearest(anyString(), anyInt(), anyDouble(), anyDouble());
    }

    @Test
    void shouldThrowWhenTaskNumberExistsOnCreate() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(true);

        assertThatThrownBy(() -> warehouseTaskService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Failed to generate unique task number after 3 attempts");

        verify(warehouseTaskRepository, times(4)).findMaxId();
        verify(warehouseTaskRepository, times(5)).existsByTaskNumber("TASK-00001");
    }

    @Test
    void shouldThrowWhenSourceLocationIsNullForPickingOnCreate() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .sourceLocationCode(null).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(2L).code("DSPS-01").build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(locationService.getByCode(request.targetLocationCode())).thenReturn(location);

        assertThatThrownBy(() -> warehouseTaskService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Source location is required for PICKING task");

        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
        verify(locationService).getByCode(request.targetLocationCode());
    }

    @Test
    void shouldThrowWhenSourceLocationIsBlankForPickingOnCreate() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .sourceLocationCode("  ").build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(2L).code("DSPS-01").build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(locationService.getByCode(request.targetLocationCode())).thenReturn(location);

        assertThatThrownBy(() -> warehouseTaskService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Source location is required for PICKING task");

        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
        verify(locationService).getByCode(request.targetLocationCode());
    }

    @Test
    void shouldThrowWhenTargetLocationIsNullForReceivingOnCreate() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .type(WarehouseTaskType.RECEIVING)
                .targetLocationCode(null).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(1L).code("PICK-01").build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(locationService.getByCode(request.sourceLocationCode())).thenReturn(location);

        assertThatThrownBy(() -> warehouseTaskService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Target location is required for RECEIVING task");

        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
        verify(locationService).getByCode(request.sourceLocationCode());
    }

    @Test
    void shouldThrowWhenTargetLocationIsBlankForReceivingOnCreate() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .type(WarehouseTaskType.RECEIVING)
                .targetLocationCode("  ").build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(1L).code("PICK-01").build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(locationService.getByCode(request.sourceLocationCode())).thenReturn(location);

        assertThatThrownBy(() -> warehouseTaskService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Target location is required for RECEIVING task");

        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
        verify(locationService).getByCode(request.sourceLocationCode());
    }

    @Test
    void shouldThrowWhenSourceLocationOrTargetIsNullForMovementOnCreate() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .type(WarehouseTaskType.MOVEMENT)
                .sourceLocationCode(null)
                .targetLocationCode(null).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);

        assertThatThrownBy(() -> warehouseTaskService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Both source and target locations are required for MOVEMENT task");

        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
    }

    @Test
    void shouldCreateTaskWithTargetLocationForReceivingOnCreate() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .type(WarehouseTaskType.RECEIVING).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(1L).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .type(WarehouseTaskType.RECEIVING).build();
        User user = UserTestData.entityBuilder().id(1L).build();
        Order order = OrderTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(locationService.getByCode(request.sourceLocationCode())).thenReturn(location);
        when(locationService.getByCode(request.targetLocationCode())).thenReturn(location);
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(orderQueryService.getOrderEntityByNumber(request.orderNumber())).thenReturn(order);
        when(warehouseTaskRepository.save(any(WarehouseTask.class))).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(any(WarehouseTask.class))).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.create(request);

        assertThat(result).isEqualTo(response);
        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
        verify(locationService).getByCode(request.sourceLocationCode());
        verify(locationService).getByCode(request.targetLocationCode());
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(orderQueryService).getOrderEntityByNumber(request.orderNumber());
        verify(warehouseTaskRepository).save(any(WarehouseTask.class));
    }

    @Test
    void shouldThrowWhenSourceLocationIsNullForMovementOnCreate() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .type(WarehouseTaskType.MOVEMENT)
                .sourceLocationCode(null).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);

        assertThatThrownBy(() -> warehouseTaskService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Both source and target locations are required for MOVEMENT task");

        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
    }

    @Test
    void shouldThrowWhenTargetLocationIsNullForMovementOnCreate() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .type(WarehouseTaskType.MOVEMENT)
                .targetLocationCode(null).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);

        assertThatThrownBy(() -> warehouseTaskService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Both source and target locations are required for MOVEMENT task");

        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
    }

    @Test
    void shouldCreateMovementTaskWhenBothLocationsProvided() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder().type(WarehouseTaskType.MOVEMENT).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .type(WarehouseTaskType.MOVEMENT).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location1 = LocationTestData.entityBuilder().id(1L).build();
        Location location2 = LocationTestData.entityBuilder().id(2L).build();
        User user = UserTestData.entityBuilder().id(1L).build();
        Order order = OrderTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(locationService.getByCode(request.sourceLocationCode())).thenReturn(location1);
        when(locationService.getByCode(request.targetLocationCode())).thenReturn(location2);
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(orderQueryService.getOrderEntityByNumber(request.orderNumber())).thenReturn(order);
        when(warehouseTaskRepository.save(any(WarehouseTask.class))).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(any(WarehouseTask.class))).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.create(request);

        assertThat(result).isEqualTo(response);
        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
        verify(locationService).getByCode(request.sourceLocationCode());
        verify(locationService).getByCode(request.targetLocationCode());
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(orderQueryService).getOrderEntityByNumber(request.orderNumber());
        verify(warehouseTaskRepository).save(any(WarehouseTask.class));
    }

    @Test
    void shouldCreateTaskWithoutAssignedUserWhenUsernameIsBlank() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder().type(WarehouseTaskType.MOVEMENT)
                .assignedUsername("  ").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .type(WarehouseTaskType.MOVEMENT)
                .assignedUsername(null)
                .build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location1 = LocationTestData.entityBuilder().id(1L).build();
        Location location2 = LocationTestData.entityBuilder().id(2L).build();
        Order order = OrderTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(locationService.getByCode(request.sourceLocationCode())).thenReturn(location1);
        when(locationService.getByCode(request.targetLocationCode())).thenReturn(location2);
        when(orderQueryService.getOrderEntityByNumber(request.orderNumber())).thenReturn(order);
        when(warehouseTaskRepository.save(any(WarehouseTask.class))).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(any(WarehouseTask.class))).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.create(request);

        ArgumentCaptor<WarehouseTask> captor = ArgumentCaptor.forClass(WarehouseTask.class);
        verify(warehouseTaskRepository).save(captor.capture());

        WarehouseTask saved = captor.getValue();
        assertThat(saved.getAssignedUser()).isNull();
        assertThat(saved.getStatus()).isEqualTo(WarehouseTaskStatus.CREATED);
        assertThat(result).isEqualTo(response);
        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
        verify(locationService).getByCode(request.sourceLocationCode());
        verify(locationService).getByCode(request.targetLocationCode());
        verify(userService, never()).getUserEntityByUserName(any());
        verify(orderQueryService).getOrderEntityByNumber(request.orderNumber());
    }

    @Test
    void shouldCreateTaskWithoutAssignedUserWhenUsernameIsNull() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder().type(WarehouseTaskType.MOVEMENT)
                .assignedUsername(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .type(WarehouseTaskType.MOVEMENT)
                .assignedUsername(null)
                .build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location1 = LocationTestData.entityBuilder().id(1L).build();
        Location location2 = LocationTestData.entityBuilder().id(2L).build();
        Order order = OrderTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(locationService.getByCode(request.sourceLocationCode())).thenReturn(location1);
        when(locationService.getByCode(request.targetLocationCode())).thenReturn(location2);
        when(orderQueryService.getOrderEntityByNumber(request.orderNumber())).thenReturn(order);
        when(warehouseTaskRepository.save(any(WarehouseTask.class))).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(any(WarehouseTask.class))).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.create(request);

        ArgumentCaptor<WarehouseTask> captor = ArgumentCaptor.forClass(WarehouseTask.class);
        verify(warehouseTaskRepository).save(captor.capture());

        WarehouseTask saved = captor.getValue();
        assertThat(saved.getAssignedUser()).isNull();
        assertThat(saved.getStatus()).isEqualTo(WarehouseTaskStatus.CREATED);
        assertThat(result).isEqualTo(response);
        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
        verify(locationService).getByCode(request.sourceLocationCode());
        verify(locationService).getByCode(request.targetLocationCode());
        verify(userService, never()).getUserEntityByUserName(any());
        verify(orderQueryService).getOrderEntityByNumber(request.orderNumber());
    }

    @Test
    void shouldCreateTaskWithoutOrderWhenOrderNumberIsNull() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder().type(WarehouseTaskType.MOVEMENT)
                .orderNumber(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .type(WarehouseTaskType.MOVEMENT)
                .orderNumber(null)
                .build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location1 = LocationTestData.entityBuilder().id(1L).build();
        Location location2 = LocationTestData.entityBuilder().id(2L).build();
        User user = UserTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(locationService.getByCode(request.sourceLocationCode())).thenReturn(location1);
        when(locationService.getByCode(request.targetLocationCode())).thenReturn(location2);
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(any(WarehouseTask.class))).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(any(WarehouseTask.class))).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.create(request);

        ArgumentCaptor<WarehouseTask> captor = ArgumentCaptor.forClass(WarehouseTask.class);
        verify(warehouseTaskRepository).save(captor.capture());

        WarehouseTask saved = captor.getValue();
        assertThat(saved.getOrder()).isNull();
        assertThat(saved.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);
        assertThat(result).isEqualTo(response);
        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
        verify(locationService).getByCode(request.sourceLocationCode());
        verify(locationService).getByCode(request.targetLocationCode());
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verifyNoInteractions(orderQueryService);
    }

    @Test
    void shouldCreateTaskWithoutOrderWhenOrderNumberIsBlank() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder().type(WarehouseTaskType.MOVEMENT)
                .orderNumber("  ").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .type(WarehouseTaskType.MOVEMENT)
                .orderNumber(null)
                .build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location1 = LocationTestData.entityBuilder().id(1L).build();
        Location location2 = LocationTestData.entityBuilder().id(2L).build();
        User user = UserTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(locationService.getByCode(request.sourceLocationCode())).thenReturn(location1);
        when(locationService.getByCode(request.targetLocationCode())).thenReturn(location2);
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(any(WarehouseTask.class))).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(any(WarehouseTask.class))).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.create(request);

        ArgumentCaptor<WarehouseTask> captor = ArgumentCaptor.forClass(WarehouseTask.class);
        verify(warehouseTaskRepository).save(captor.capture());

        WarehouseTask saved = captor.getValue();
        assertThat(saved.getOrder()).isNull();
        assertThat(saved.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);
        assertThat(result).isEqualTo(response);
        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
        verify(locationService).getByCode(request.sourceLocationCode());
        verify(locationService).getByCode(request.targetLocationCode());
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verifyNoInteractions(orderQueryService);
    }

    @Test
    void shouldNotReserveStockWhenSkipReserveIsTrue() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder().skipReserve(true).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location1 = LocationTestData.entityBuilder().id(1L).build();
        Location location2 = LocationTestData.entityBuilder().id(2L).build();
        User user = UserTestData.entityBuilder().id(1L).build();
        Order order = OrderTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findMaxId()).thenReturn(Optional.of(0L));
        when(warehouseTaskRepository.existsByTaskNumber("TASK-00001")).thenReturn(false);
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(locationService.getByCode(request.sourceLocationCode())).thenReturn(location1);
        when(locationService.getByCode(request.targetLocationCode())).thenReturn(location2);
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(orderQueryService.getOrderEntityByNumber(request.orderNumber())).thenReturn(order);
        when(warehouseTaskRepository.save(any(WarehouseTask.class))).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(any(WarehouseTask.class))).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.create(request);

        assertThat(result).isEqualTo(response);
        verify(warehouseTaskRepository).findMaxId();
        verify(warehouseTaskRepository, times(2)).existsByTaskNumber("TASK-00001");
        verify(productService).getBySku(request.productSku());
        verify(locationService).getByCode(request.sourceLocationCode());
        verify(locationService).getByCode(request.targetLocationCode());
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(orderQueryService).getOrderEntityByNumber(request.orderNumber());
        verify(warehouseTaskRepository).save(any(WarehouseTask.class));
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldReturnWarehouseTaskById() {
        Long id = 1L;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.getById(id);

        assertThat(result).isEqualTo(response);
        verify(warehouseTaskRepository).findById(id);
    }

    @Test
    void shouldThrowWhenWarehouseTaskNotFound() {
        Long id = 1L;

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseTaskService.getById(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Task with id: %s not found", id);

        verify(warehouseTaskRepository).findById(id);
    }

    @Test
    void shouldReturnPageOfWarehouseTasks() {
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();

        Pageable pageable = PageRequest.of(0, 10);
        Page<WarehouseTask> page = new PageImpl<>(List.of(warehouseTask), pageable, 1);

        when(warehouseTaskRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        Page<WarehouseTaskResponse> result = warehouseTaskService.getAll(0, 10, "createdAt", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(warehouseTaskRepository).findAll(any(Pageable.class));
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
    }

    @Test
    void shouldReturnPageOfWarehouseTasksFilteredByStatus() {
        WarehouseTaskStatus status = WarehouseTaskStatus.CREATED;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();

        Pageable pageable = PageRequest.of(0, 10);
        Page<WarehouseTask> page = new PageImpl<>(List.of(warehouseTask), pageable, 1);

        when(warehouseTaskRepository.findByStatus(eq(status), any(Pageable.class))).thenReturn(page);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        Page<WarehouseTaskResponse> result = warehouseTaskService.getByStatus(status, 0, 10, "createdAt", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(warehouseTaskRepository).findByStatus(eq(status), any(Pageable.class));
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
    }

    @Test
    void shouldReturnPageOfWarehouseTasksFilteredByQuery() {
        String query = "query";
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();

        Pageable pageable = PageRequest.of(0, 10);
        Page<WarehouseTask> page = new PageImpl<>(List.of(warehouseTask), pageable, 1);

        when(warehouseTaskRepository.search(eq(query), any(Pageable.class))).thenReturn(page);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        Page<WarehouseTaskResponse> result = warehouseTaskService.search(query, 0, 10, "createdAt", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(warehouseTaskRepository).search(eq(query), any(Pageable.class));
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
    }

    @Test
    void shouldReturnTrueWhenAllTasksAreCompleted() {
        String orderNumber = "ORD-00001";
        WarehouseTaskType taskType = WarehouseTaskType.PICKING;
        WarehouseTask task1 = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTask task2 = WarehouseTaskTestData.entityBuilder().id(2L).build();
        WarehouseTask task3 = WarehouseTaskTestData.entityBuilder().id(3L).build();

        when(warehouseTaskRepository.findByOrderNumberAndType(orderNumber, taskType)).thenReturn(List.of(task1, task2, task3));

        boolean result = warehouseTaskService.areAllTasksCompletedForOrder(orderNumber, taskType);

        assertThat(result).isTrue();
        verify(warehouseTaskRepository).findByOrderNumberAndType(orderNumber, taskType);
    }

    @Test
    void shouldReturnFalseWhenNotAllTasksAreCompleted() {
        String orderNumber = "ORD-00001";
        WarehouseTaskType taskType = WarehouseTaskType.PICKING;
        WarehouseTask task1 = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTask task2 = WarehouseTaskTestData.entityBuilder().id(2L).status(WarehouseTaskStatus.IN_PROGRESS).build();
        WarehouseTask task3 = WarehouseTaskTestData.entityBuilder().id(3L).build();

        when(warehouseTaskRepository.findByOrderNumberAndType(orderNumber, taskType)).thenReturn(List.of(task1, task2, task3));

        boolean result = warehouseTaskService.areAllTasksCompletedForOrder(orderNumber, taskType);

        assertThat(result).isFalse();
        verify(warehouseTaskRepository).findByOrderNumberAndType(orderNumber, taskType);
    }

    @Test
    void shouldReturnTrueAndIgnoreCancelledTasks() {
        String orderNumber = "ORD-00001";
        WarehouseTaskType taskType = WarehouseTaskType.PICKING;
        WarehouseTask task1 = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTask task2 = WarehouseTaskTestData.entityBuilder().id(2L).status(WarehouseTaskStatus.CANCELLED).build();
        WarehouseTask task3 = WarehouseTaskTestData.entityBuilder().id(3L).build();

        when(warehouseTaskRepository.findByOrderNumberAndType(orderNumber, taskType)).thenReturn(List.of(task1, task2, task3));

        boolean result = warehouseTaskService.areAllTasksCompletedForOrder(orderNumber, taskType);

        assertThat(result).isTrue();
        verify(warehouseTaskRepository).findByOrderNumberAndType(orderNumber, taskType);
    }

    @Test
    void shouldUpdateWarehouseTask() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(orderItemService.getRemainingQuantity(warehouseTask.getOrder(), warehouseTask.getProduct())).thenReturn(20);
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(orderItemService).getRemainingQuantity(warehouseTask.getOrder(), warehouseTask.getProduct());
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(locationService);
        verifyNoInteractions(productService);
    }

    @Test
    void shouldThrowWhenStatusIsNotCreated() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot update task with status: %s", warehouseTask.getStatus());

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(warehouseTaskMapper);
        verifyNoInteractions(orderItemService);
    }

    @Test
    void shouldUpdateWhenWarehouseTypeIsNull() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .type(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(orderItemService.getRemainingQuantity(warehouseTask.getOrder(), warehouseTask.getProduct())).thenReturn(20);
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getType()).isEqualTo(WarehouseTaskType.PICKING);
        verify(warehouseTaskRepository).findById(id);
        verify(orderItemService).getRemainingQuantity(warehouseTask.getOrder(), warehouseTask.getProduct());
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(locationService);
        verifyNoInteractions(productService);
    }

    @Test
    void shouldUpdateWhenPlannedQuantityIsNull() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder().plannedQuantity(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);
        assertThat(warehouseTask.getPlannedQuantity()).isEqualTo(10);
        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(locationService);
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemService);
    }

    @Test
    void shouldThrowWhenPlannedQuantityIsNegative() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder().plannedQuantity(-10).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Quantity must be positive");

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(warehouseTaskMapper);
        verifyNoInteractions(orderItemService);
    }

    @Test
    void shouldThrowWhenPlannedQuantityGreaterThanRemainingOrderQuantity() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder().plannedQuantity(15).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(orderItemService.getRemainingQuantity(warehouseTask.getOrder(), warehouseTask.getProduct())).thenReturn(10);

        assertThatThrownBy(() -> warehouseTaskService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot exceed remaining order quantity");

        verify(warehouseTaskRepository).findById(id);
        verify(orderItemService).getRemainingQuantity(warehouseTask.getOrder(), warehouseTask.getProduct());
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(warehouseTaskMapper);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldUpdateTaskWhenOrderIsNull() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder().plannedQuantity(100).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).order(null).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);
        assertThat(warehouseTask.getPlannedQuantity()).isEqualTo(100);
        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(locationService);
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemService);
    }

    @Test
    void shouldUpdateTaskWhenProductIsNull() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(100).productSku(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).product(null).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);
        assertThat(warehouseTask.getPlannedQuantity()).isEqualTo(100);
        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(locationService);
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemService);
    }

    @Test
    void shouldNotUpdateConfirmedQuantityWhenItIsNull() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder().plannedQuantity(null).confirmedQuantity(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);
        assertThat(warehouseTask.getConfirmedQuantity()).isEqualTo(10);
        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(locationService);
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemService);
    }

    @Test
    void shouldThrowWhenChangingProductForTaskWithOrder() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).productSku("NEW-SKU-123").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));


        assertThatThrownBy(() -> warehouseTaskService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot change product for task linked to order. Cancel this task and create a new one.");

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(warehouseTaskMapper);
        verifyNoInteractions(orderItemService);
    }

    @Test
    void shouldUpdateProductWhenTaskHasNoOrder() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).productSku("NEW-SKU-123").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().order(null).id(1L)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .productSku("NEW-SKU-123").build();
        User user = UserTestData.entityBuilder().id(1L).build();
        Product product = ProductTestData.entityBuilder().id(1L).sku("NEW-SKU-123").build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(productService.getBySku(request.productSku())).thenReturn(product);
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);
        ArgumentCaptor<WarehouseTask> captor = ArgumentCaptor.forClass(WarehouseTask.class);
        verify(warehouseTaskRepository).save(captor.capture());

        WarehouseTask saved = captor.getValue();
        assertThat(saved.getOrder()).isNull();
        assertThat(saved.getProduct()).isEqualTo(product);
        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);
        verify(warehouseTaskRepository).findById(id);
        verify(productService).getBySku(request.productSku());
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(locationService);
        verifyNoInteractions(orderItemService);
    }

    @Test
    void shouldNotUpdateProductWhenProductSkuIsBlank() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).productSku("   ").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);
        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(locationService);
        verifyNoInteractions(orderItemService);
        verifyNoInteractions(productService);
    }

    @Test
    void shouldUpdateSourceLocationWhenDifferentCodeProvided() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).sourceLocationCode("NEW-PICK").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(2L).code("NEW-PICK").build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(locationService.getByCode(request.sourceLocationCode())).thenReturn(location);
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        ArgumentCaptor<WarehouseTask> captor = ArgumentCaptor.forClass(WarehouseTask.class);
        verify(warehouseTaskRepository).save(captor.capture());
        WarehouseTask saved = captor.getValue();

        assertThat(saved.getSourceLocation()).isEqualTo(location);
        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verify(locationService).getByCode(request.sourceLocationCode());
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemService);
    }

    @Test
    void shouldNotUpdateSourceLocationWhenCodeIsBlank() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).sourceLocationCode("  ").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemService);
        verifyNoInteractions(locationService);
    }

    @Test
    void shouldNotUpdateSourceLocationWhenCodeIsNull() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).sourceLocationCode(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemService);
        verifyNoInteractions(locationService);
    }

    @Test
    void shouldUpdateSourceLocationWhenCurrentLocationIsNull() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).sourceLocationCode("NEW-PICK").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).sourceLocation(null).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(2L).code("NEW-PICK").build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(locationService.getByCode(request.sourceLocationCode())).thenReturn(location);
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        ArgumentCaptor<WarehouseTask> captor = ArgumentCaptor.forClass(WarehouseTask.class);
        verify(warehouseTaskRepository).save(captor.capture());
        WarehouseTask saved = captor.getValue();

        assertThat(saved.getSourceLocation()).isEqualTo(location);
        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(locationService).getByCode(request.sourceLocationCode());
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemService);
    }

    @Test
    void shouldUpdateTargetLocationWhenDifferentCodeProvided() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).targetLocationCode("NEW-PICK").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder().targetLocationCode("NEW-PICK").build();
        User user = UserTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(2L).code("NEW-PICK").build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(locationService.getByCode(request.targetLocationCode())).thenReturn(location);
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        ArgumentCaptor<WarehouseTask> captor = ArgumentCaptor.forClass(WarehouseTask.class);
        verify(warehouseTaskRepository).save(captor.capture());
        WarehouseTask saved = captor.getValue();

        assertThat(saved.getTargetLocation()).isEqualTo(location);
        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verify(locationService).getByCode(request.targetLocationCode());
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemService);
    }

    @Test
    void shouldNotUpdateTargetLocationWhenCodeIsBlank() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).targetLocationCode("  ").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemService);
        verifyNoInteractions(locationService);
    }

    @Test
    void shouldNotUpdateTargetLocationWhenCodeIsNull() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).targetLocationCode(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemService);
        verifyNoInteractions(locationService);
    }

    @Test
    void shouldUpdateTargetLocationWhenCurrentLocationIsNull() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).targetLocationCode("NEW-PICK").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).targetLocation(null).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder().targetLocationCode("NEW-PICK").build();
        User user = UserTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(2L).code("NEW-PICK").build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(locationService.getByCode(request.targetLocationCode())).thenReturn(location);
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        ArgumentCaptor<WarehouseTask> captor = ArgumentCaptor.forClass(WarehouseTask.class);
        verify(warehouseTaskRepository).save(captor.capture());
        WarehouseTask saved = captor.getValue();

        assertThat(saved.getTargetLocation()).isEqualTo(location);
        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(locationService).getByCode(request.targetLocationCode());
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(productService);
        verifyNoInteractions(orderItemService);
    }

    @Test
    void shouldThrowWhenChangingOrderForExistingTask() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).orderNumber("NEW-ORD-999").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot change order for existing task");

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(warehouseTaskMapper);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldUpdateOrderWhenTaskHasNoOrder() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).assignedUsername(null).orderNumber("NEW-ORD-999").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .order(null).status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .orderNumber("NEW-ORD-999").assignedUsername(null).build();
        Order order = OrderTestData.entityBuilder().id(2L).orderNumber("NEW-ORD-999").build();


        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(orderQueryService.getOrderEntityByNumber(request.orderNumber())).thenReturn(order);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        ArgumentCaptor<WarehouseTask> captor = ArgumentCaptor.forClass(WarehouseTask.class);
        verify(warehouseTaskRepository).save(captor.capture());
        WarehouseTask saved = captor.getValue();

        assertThat(saved.getOrder()).isEqualTo(order);
        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(orderQueryService).getOrderEntityByNumber(request.orderNumber());
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(productService);
        verifyNoInteractions(locationService);
        verifyNoInteractions(userService);
    }

    @Test
    void shouldNotUpdateOrderWhenOrderNumberIsNull() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).assignedUsername(null).orderNumber(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .order(null).status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .assignedUsername(null).build();


        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verify(warehouseTaskRepository).save(warehouseTask);
        verifyNoInteractions(productService);
        verifyNoInteractions(locationService);
        verifyNoInteractions(userService);
        verifyNoInteractions(orderQueryService);
    }

    @Test
    void shouldNotUpdateOrderWhenOrderNumberIsBlank() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).assignedUsername(null).orderNumber("   ").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .order(null).status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .assignedUsername(null).build();


        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verify(warehouseTaskRepository).save(warehouseTask);
        verifyNoInteractions(productService);
        verifyNoInteractions(locationService);
        verifyNoInteractions(userService);
        verifyNoInteractions(orderQueryService);
    }

    @Test
    void shouldUpdateAssignedUserWhenUsernameProvided() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).assignedUsername("username").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .assignedUser(null).status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .assignedUsername("username").build();
        User user = UserTestData.entityBuilder().id(1L).username("username").build();


        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(userService.getUserEntityByUserName(request.assignedUsername())).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        ArgumentCaptor<WarehouseTask> captor = ArgumentCaptor.forClass(WarehouseTask.class);
        verify(warehouseTaskRepository).save(captor.capture());
        WarehouseTask saved = captor.getValue();

        assertThat(saved.getAssignedUser()).isEqualTo(user);
        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(request.assignedUsername());
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(productService);
        verifyNoInteractions(locationService);
        verifyNoInteractions(orderQueryService);
    }

    @Test
    void shouldNotUpdateAssignedUserWhenUsernameIsBlankOnUpdate() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).assignedUsername("  ").build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .assignedUser(null).status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .assignedUsername(null).build();


        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.CREATED);

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(productService);
        verifyNoInteractions(locationService);
        verifyNoInteractions(orderQueryService);
        verifyNoInteractions(userService);
    }

    @Test
    void shouldThrowWhenPickingTaskHasNoSourceLocationOnUpdate() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).assignedUsername(null).sourceLocationCode(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .sourceLocation(null).type(WarehouseTaskType.PICKING).status(WarehouseTaskStatus.CREATED).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Source location is required for PICKING task");

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(warehouseTask);
        verifyNoInteractions(stockService);
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldThrowWhenReceivingTaskHasNoTargetLocationOnUpdate() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).assignedUsername(null).targetLocationCode(null).type(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .targetLocation(null).type(WarehouseTaskType.RECEIVING).status(WarehouseTaskStatus.CREATED).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Target location is required for RECEIVING task");

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(warehouseTask);
        verifyNoInteractions(stockService);
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldThrowWhenMovementTaskHasNoLocationsOnUpdate() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).assignedUsername(null).sourceLocationCode(null).targetLocationCode(null)
                .type(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).sourceLocation(null)
                .targetLocation(null).type(WarehouseTaskType.MOVEMENT).status(WarehouseTaskStatus.CREATED).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Both source and target locations are required for MOVEMENT task");

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(warehouseTask);
        verifyNoInteractions(stockService);
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldUpdateReceivingTaskSuccessfully() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).assignedUsername(null).type(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).type(WarehouseTaskType.RECEIVING)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .type(WarehouseTaskType.RECEIVING)
                .build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(productService);
        verifyNoInteractions(locationService);
        verifyNoInteractions(orderQueryService);
        verifyNoInteractions(userService);
    }

    @Test
    void shouldUpdateMovementTaskSuccessfully() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).assignedUsername(null).type(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).type(WarehouseTaskType.MOVEMENT)
                .status(WarehouseTaskStatus.CREATED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .type(WarehouseTaskType.MOVEMENT)
                .build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
        verifyNoInteractions(productService);
        verifyNoInteractions(locationService);
        verifyNoInteractions(orderQueryService);
        verifyNoInteractions(userService);
    }

    @Test
    void shouldThrowWhenTargetLocationIsNullForMovementOnUpdate() {
        Long id = 1L;
        WarehouseTaskRequest request = WarehouseTaskTestData.request().toBuilder()
                .plannedQuantity(null).assignedUsername(null).targetLocationCode(null).type(null).build();
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .targetLocation(null).type(WarehouseTaskType.MOVEMENT).status(WarehouseTaskStatus.CREATED).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Both source and target locations are required for MOVEMENT task");

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(warehouseTask);
        verifyNoInteractions(stockService);
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldDeleteTask() {
        Long id = 1L;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        warehouseTaskService.delete(id);

        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.CANCELLED);
        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository).save(warehouseTask);
    }

    @Test
    void shouldDeletePickingTaskWithReservedStock() {
        Long id = 1L;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).confirmedQuantity(5).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        warehouseTaskService.delete(id);

        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.CANCELLED);
        verify(warehouseTaskRepository).findById(id);
        verify(stockService).releaseReserved("BRP-ENG-001", 5);
        verify(warehouseTaskRepository).save(warehouseTask);
    }

    @Test
    void shouldDeletePickingTaskWithNullConfirmedQuantity() {
        Long id = 1L;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).confirmedQuantity(null).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        warehouseTaskService.delete(id);

        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.CANCELLED);
        verify(warehouseTaskRepository).findById(id);
        verify(stockService).releaseReserved("BRP-ENG-001", 10);
        verify(warehouseTaskRepository).save(warehouseTask);
    }

    @Test
    void shouldThrowWhenTaskNotFoundOnDelete() {
        Long id = 1L;

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseTaskService.delete(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Task with id: %s not found", id);

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldThrowWhenStatusIncorrect() {
        Long id = 1L;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.COMPLETED).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.delete(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot cancel task with status: %s", warehouseTask.getStatus());

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldDeleteReceivingTask() {
        Long id = 1L;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).type(WarehouseTaskType.RECEIVING).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        warehouseTaskService.delete(id);

        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.CANCELLED);
        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository).save(warehouseTask);
        verifyNoInteractions(stockService);
    }

    @Test
    void shouldDeleteTaskWithCreatedStatus() {
        Long id = 1L;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).type(WarehouseTaskType.RECEIVING).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        warehouseTaskService.delete(id);

        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.CANCELLED);
        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository).save(warehouseTask);
    }

    @Test
    void shouldDeleteTaskWithAssignedStatus() {
        Long id = 1L;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.ASSIGNED).type(WarehouseTaskType.RECEIVING).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        warehouseTaskService.delete(id);

        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.CANCELLED);
        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository).save(warehouseTask);
    }

    @Test
    void shouldAssignTask() {
        Long id = 1L;
        String username = "username";
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();
        User user = UserTestData.entityBuilder().id(1L).username(username).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .status(WarehouseTaskStatus.ASSIGNED)
                .assignedUsername(username).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(userService.getUserEntityByUserName(username)).thenReturn(user);
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.assignTask(id, username);

        ArgumentCaptor<WarehouseTask> captor = ArgumentCaptor.forClass(WarehouseTask.class);
        verify(warehouseTaskRepository).save(captor.capture());
        WarehouseTask saved = captor.getValue();

        assertThat(result).isEqualTo(response);
        assertThat(saved.getStatus()).isEqualTo(WarehouseTaskStatus.ASSIGNED);
        assertThat(saved.getAssignedUser().getUsername()).isEqualTo(username);

        verify(warehouseTaskRepository).findById(id);
        verify(userService).getUserEntityByUserName(username);
    }

    @Test
    void shouldThrowWhenAssigningTaskWithInvalidStatus() {
        Long id = 1L;
        String username = "username";
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.ASSIGNED).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.assignTask(id, username))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot assign task with status: %s", warehouseTask.getStatus());

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(userService);
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldTakeAllTasks() {
        String username = "username";
        WarehouseTask task1 = WarehouseTaskTestData.entityBuilder().id(1L).status(WarehouseTaskStatus.CREATED).build();
        WarehouseTask task2 = WarehouseTaskTestData.entityBuilder().id(2L).status(WarehouseTaskStatus.CREATED).build();

        WarehouseTaskResponse response1 = WarehouseTaskTestData.response().toBuilder().id(1L).build();
        WarehouseTaskResponse response2 = WarehouseTaskTestData.response().toBuilder().id(2L).build();

        User user = UserTestData.entityBuilder().username("username").build();
        Page<WarehouseTask> page = new PageImpl<>(List.of(task1, task2));

        when(warehouseTaskRepository.findByStatus(eq(WarehouseTaskStatus.CREATED), any(Pageable.class))).thenReturn(page);
        when(warehouseTaskMapper.toResponseDto(task1)).thenReturn(response1);
        when(warehouseTaskMapper.toResponseDto(task2)).thenReturn(response2);
        when(warehouseTaskRepository.findById(1L)).thenReturn(Optional.of(task1));
        when(warehouseTaskRepository.findById(2L)).thenReturn(Optional.of(task2));
        when(userService.getUserEntityByUserName("username")).thenReturn(user);

        int result = warehouseTaskService.takeAllTasks(user.getUsername());

        assertThat(result).isEqualTo(2);
        verify(warehouseTaskRepository, times(2)).save(any(WarehouseTask.class));
        verify(userService, times(2)).getUserEntityByUserName(username);
    }

    @Test
    void shouldTakeOnlyAvailableTasksWhenSomeFail() {
        String username = "username";
        WarehouseTask task1 = WarehouseTaskTestData.entityBuilder().id(1L).status(WarehouseTaskStatus.CREATED).build();
        WarehouseTask task2 = WarehouseTaskTestData.entityBuilder().id(2L).status(WarehouseTaskStatus.IN_PROGRESS).build();

        WarehouseTaskResponse response1 = WarehouseTaskTestData.response().toBuilder().id(1L).build();
        WarehouseTaskResponse response2 = WarehouseTaskTestData.response().toBuilder().id(2L).build();

        User user = UserTestData.entityBuilder().username("username").build();
        Page<WarehouseTask> page = new PageImpl<>(List.of(task1, task2));

        when(warehouseTaskRepository.findByStatus(eq(WarehouseTaskStatus.CREATED), any(Pageable.class))).thenReturn(page);
        when(warehouseTaskMapper.toResponseDto(task1)).thenReturn(response1);
        when(warehouseTaskMapper.toResponseDto(task2)).thenReturn(response2);
        when(warehouseTaskRepository.findById(1L)).thenReturn(Optional.of(task1));
        when(warehouseTaskRepository.findById(2L)).thenReturn(Optional.of(task2));
        when(userService.getUserEntityByUserName("username")).thenReturn(user);

        int result = warehouseTaskService.takeAllTasks(user.getUsername());

        assertThat(result).isEqualTo(1);
        verify(warehouseTaskRepository, times(1)).save(any(WarehouseTask.class));
        verify(userService, times(1)).getUserEntityByUserName(username);
    }

    @Test
    void shouldStartTask() {
        Long id = 1L;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.ASSIGNED).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .status(WarehouseTaskStatus.IN_PROGRESS).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.startTask(id);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.IN_PROGRESS);
        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository).save(warehouseTask);
    }

    @Test
    void shouldThrowWhenStartingTaskWithInvalidStatus() {
        Long id = 1L;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.CREATED).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.startTask(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot start task with status: %s", warehouseTask.getStatus());

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldCompletePickingTask() {
        Long id = 1L;
        Integer confirmedQuantity = 10;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).confirmedQuantity(0).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .status(WarehouseTaskStatus.COMPLETED).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.completeTask(id, confirmedQuantity);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.COMPLETED);
        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(taskCompletionService).finalizeTask(warehouseTask.getId(), 10);
        verify(eventPublisher).publishEvent(any(TaskCompletedEvent.class));
        verify(stockService).decreaseStock("BRP-ENG-001", "PICK-01", 10);
    }

    @Test
    void shouldCompleteMovementTask() {
        Long id = 1L;
        Integer confirmedQuantity = 10;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).type(WarehouseTaskType.MOVEMENT).confirmedQuantity(0).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .status(WarehouseTaskStatus.COMPLETED).type(WarehouseTaskType.MOVEMENT).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.completeTask(id, confirmedQuantity);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.COMPLETED);
        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(taskCompletionService).finalizeTask(warehouseTask.getId(), 10);
        verify(eventPublisher).publishEvent(any(TaskCompletedEvent.class));
        verify(stockService).transferStock("BRP-ENG-001", 10, "PICK-01", "DSPS-01");
    }

    @Test
    void shouldCompleteReceivingTask() {
        Long id = 1L;
        Integer confirmedQuantity = 10;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).type(WarehouseTaskType.RECEIVING).confirmedQuantity(0).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .status(WarehouseTaskStatus.COMPLETED).type(WarehouseTaskType.RECEIVING).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.completeTask(id, confirmedQuantity);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.COMPLETED);
        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(taskCompletionService).finalizeTask(warehouseTask.getId(), 10);
        verify(eventPublisher).publishEvent(any(TaskCompletedEvent.class));
        verify(stockService).increaseStock("BRP-ENG-001", "DSPS-01", 10);
    }

    @Test
    void shouldCompleteTaskWithNullConfirmedQuantity() {
        Long id = 1L;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).confirmedQuantity(0).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .status(WarehouseTaskStatus.COMPLETED).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));
        when(warehouseTaskRepository.save(warehouseTask)).thenReturn(warehouseTask);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        WarehouseTaskResponse result = warehouseTaskService.completeTask(id, null);

        assertThat(result).isEqualTo(response);
        assertThat(warehouseTask.getStatus()).isEqualTo(WarehouseTaskStatus.COMPLETED);
        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository).save(warehouseTask);
        verify(taskCompletionService).finalizeTask(warehouseTask.getId(), 10);
        verify(eventPublisher).publishEvent(any(TaskCompletedEvent.class));
        verify(stockService).decreaseStock("BRP-ENG-001", "PICK-01", 10);
    }

    @Test
    void shouldThrowWhenConfirmedQuantityIsNegative() {
        Long id = 1L;
        Integer confirmedQuantity = -5;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).confirmedQuantity(0).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.completeTask(id, confirmedQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Confirmed quantity cannot be negative");

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(taskCompletionService);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(stockService);
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldThrowWhenCompletingTaskWithInvalidStatus() {
        Long id = 1L;
        Integer confirmedQuantity = 10;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.ASSIGNED).confirmedQuantity(0).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.completeTask(id, confirmedQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Cannot complete task with status: %s", warehouseTask.getStatus());

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(taskCompletionService);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(stockService);
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldThrowWhenTaskNotFoundOnComplete() {
        Long id = 99L;
        Integer confirmedQuantity = 10;

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseTaskService.completeTask(id, confirmedQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Task with id: %s not found", id);

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(taskCompletionService);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(stockService);
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldThrowWhenCompletingPickingTaskWithoutSourceLocation() {
        Long id = 1L;
        Integer confirmedQuantity = 10;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).sourceLocation(null).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.completeTask(id, confirmedQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Source location is required for PICKING task");

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(taskCompletionService);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(stockService);
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldThrowWhenCompletingMovementTaskWithoutSourceLocation() {
        Long id = 1L;
        Integer confirmedQuantity = 10;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).type(WarehouseTaskType.MOVEMENT).sourceLocation(null).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.completeTask(id, confirmedQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Both locations are required for MOVEMENT task");

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(taskCompletionService);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(stockService);
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldThrowWhenCompletingMovementTaskWithoutTargetLocation() {
        Long id = 1L;
        Integer confirmedQuantity = 10;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).type(WarehouseTaskType.MOVEMENT).targetLocation(null).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.completeTask(id, confirmedQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Both locations are required for MOVEMENT task");

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(taskCompletionService);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(stockService);
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldThrowWhenCompletingReceivingTaskWithoutTargetLocation() {
        Long id = 1L;
        Integer confirmedQuantity = 10;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).type(WarehouseTaskType.RECEIVING).targetLocation(null).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.completeTask(id, confirmedQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Target location is required for RECEIVING task");

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(taskCompletionService);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(stockService);
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldThrowWhenTypeIncorrect() {
        Long id = 1L;
        Integer confirmedQuantity = 10;
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L)
                .status(WarehouseTaskStatus.IN_PROGRESS).type(null).build();

        when(warehouseTaskRepository.findById(id)).thenReturn(Optional.of(warehouseTask));

        assertThatThrownBy(() -> warehouseTaskService.completeTask(id, confirmedQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Unsupported task type for stock update: %s", warehouseTask.getType());

        verify(warehouseTaskRepository).findById(id);
        verify(warehouseTaskRepository, never()).save(any());
        verifyNoInteractions(taskCompletionService);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(stockService);
        verifyNoInteractions(warehouseTaskMapper);
    }


    @Test
    void shouldReturnTasksByUserAndStatuses() {
        String username = "username";
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        List<WarehouseTaskStatus> statuses = List.of(
                WarehouseTaskStatus.CREATED,
                WarehouseTaskStatus.ASSIGNED,
                WarehouseTaskStatus.IN_PROGRESS,
                WarehouseTaskStatus.COMPLETED
        );

        User user = UserTestData.entityBuilder().id(1L).username("username").build();


        Pageable pageable = PageRequest.of(0, 10);
        Page<WarehouseTask> page = new PageImpl<>(List.of(warehouseTask), pageable, 1);

        when(userService.getUserEntityByUserName(username)).thenReturn(user);
        when(warehouseTaskRepository.findByAssignedUserIdAndStatusIn(eq(1L), eq(statuses), any(Pageable.class)))
                .thenReturn(page);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        Page<WarehouseTaskResponse> result = warehouseTaskService.getTasksByUserAndStatus(username, statuses, 0,
                10, "createdAt", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(userService).getUserEntityByUserName(username);
        verify(warehouseTaskRepository).findByAssignedUserIdAndStatusIn(eq(1L), eq(statuses), any(Pageable.class));
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
    }

    @Test
    void shouldReturnTasksWithDefaultStatusesWhenStatusesIsNull() {
        String username = "username";
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).username("username").build();

        List<WarehouseTaskStatus> expectedDefaultStatuses = List.of(
                WarehouseTaskStatus.CREATED,
                WarehouseTaskStatus.ASSIGNED,
                WarehouseTaskStatus.IN_PROGRESS,
                WarehouseTaskStatus.COMPLETED
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<WarehouseTask> page = new PageImpl<>(List.of(warehouseTask), pageable, 1);

        when(userService.getUserEntityByUserName(username)).thenReturn(user);

        when(warehouseTaskRepository.findByAssignedUserIdAndStatusIn(eq(1L), eq(expectedDefaultStatuses), any(Pageable.class)))
                .thenReturn(page);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        Page<WarehouseTaskResponse> result = warehouseTaskService.getTasksByUserAndStatus(username, null, 0,
                10, "createdAt", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);

        verify(userService).getUserEntityByUserName(username);
        verify(warehouseTaskRepository).findByAssignedUserIdAndStatusIn(eq(1L), eq(expectedDefaultStatuses), any(Pageable.class));
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
    }

    @Test
    void shouldReturnTasksWithDefaultStatusesWhenStatusesIsEmpty() {
        String username = "username";
        WarehouseTask warehouseTask = WarehouseTaskTestData.entityBuilder().id(1L).build();
        WarehouseTaskResponse response = WarehouseTaskTestData.response();
        User user = UserTestData.entityBuilder().id(1L).username("username").build();

        List<WarehouseTaskStatus> expectedDefaultStatuses = List.of(
                WarehouseTaskStatus.CREATED,
                WarehouseTaskStatus.ASSIGNED,
                WarehouseTaskStatus.IN_PROGRESS,
                WarehouseTaskStatus.COMPLETED
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<WarehouseTask> page = new PageImpl<>(List.of(warehouseTask), pageable, 1);

        when(userService.getUserEntityByUserName(username)).thenReturn(user);

        when(warehouseTaskRepository.findByAssignedUserIdAndStatusIn(eq(1L), eq(expectedDefaultStatuses), any(Pageable.class)))
                .thenReturn(page);
        when(warehouseTaskMapper.toResponseDto(warehouseTask)).thenReturn(response);

        Page<WarehouseTaskResponse> result = warehouseTaskService.getTasksByUserAndStatus(username, List.of(), 0,
                10, "createdAt", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);

        verify(userService).getUserEntityByUserName(username);
        verify(warehouseTaskRepository).findByAssignedUserIdAndStatusIn(eq(1L), eq(expectedDefaultStatuses), any(Pageable.class));
        verify(warehouseTaskMapper).toResponseDto(warehouseTask);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnGetTasksByUserAndStatus() {
        String username = "username";

        List<WarehouseTaskStatus> statuses = List.of(
                WarehouseTaskStatus.CREATED,
                WarehouseTaskStatus.ASSIGNED,
                WarehouseTaskStatus.IN_PROGRESS,
                WarehouseTaskStatus.COMPLETED
        );

        when(userService.getUserEntityByUserName(username)).thenThrow(new CommonBackendException(
                String.format("User with username '%s' not found", username), HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> warehouseTaskService.getTasksByUserAndStatus(username, statuses, 0,
                10, "createdAt", Sort.Direction.ASC))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("User with username '%s' not found", username);

        verify(userService).getUserEntityByUserName(username);
        verifyNoInteractions(warehouseTaskRepository);
        verifyNoInteractions(warehouseTaskMapper);
    }

    @Test
    void shouldReturnTrueWhenActiveTasksExist() {
        String orderNumber = "ORD-00001";

        List<WarehouseTaskStatus> expectedStatuses = List.of(
                WarehouseTaskStatus.CREATED,
                WarehouseTaskStatus.ASSIGNED,
                WarehouseTaskStatus.IN_PROGRESS
        );

        when(warehouseTaskRepository.existsByOrderOrderNumberAndStatusIn(eq(orderNumber), eq(expectedStatuses))).thenReturn(true);

        boolean result = warehouseTaskService.hasActiveTasksForOrder(orderNumber);

        assertThat(result).isTrue();
        verify(warehouseTaskRepository).existsByOrderOrderNumberAndStatusIn(eq(orderNumber), eq(expectedStatuses));
    }

    @Test
    void shouldReturnFalseWhenNoActiveTasksExist() {
        String orderNumber = "ORD-00001";

        List<WarehouseTaskStatus> expectedStatuses = List.of(
                WarehouseTaskStatus.CREATED,
                WarehouseTaskStatus.ASSIGNED,
                WarehouseTaskStatus.IN_PROGRESS
        );

        when(warehouseTaskRepository.existsByOrderOrderNumberAndStatusIn(eq(orderNumber), eq(expectedStatuses))).thenReturn(false);

        boolean result = warehouseTaskService.hasActiveTasksForOrder(orderNumber);

        assertThat(result).isFalse();
        verify(warehouseTaskRepository).existsByOrderOrderNumberAndStatusIn(eq(orderNumber), eq(expectedStatuses));
    }
}