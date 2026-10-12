package ru.tsvetikov.warehouse.router.testdata;

import ru.tsvetikov.warehouse.router.model.db.entity.WarehouseTask;
import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.WarehouseTaskResponse;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskStatus;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;

import java.time.LocalDateTime;

public final class WarehouseTaskTestData {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 15, 9, 0);
    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 1, 15, 10, 0);
    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 1, 15, 12, 0);

    private WarehouseTaskTestData() {
    }

    public static WarehouseTaskRequest request() {
        return new WarehouseTaskRequest(WarehouseTaskType.PICKING, "BRP-ENG-001", 10,
                "PICK-01", "DSPS-01", "username",
                "ORD-00001", 10, false);
    }

    public static WarehouseTaskRequest request(String sku, String orderNumber) {
        return new WarehouseTaskRequest(WarehouseTaskType.PICKING, sku, 10,
                "PICK-01", "DSPS-01", "username",
                orderNumber, 10, false);
    }

    public static WarehouseTask.WarehouseTaskBuilder entityBuilder() {
        return WarehouseTask.builder()
                .taskNumber("TASK-00001")
                .type(WarehouseTaskType.PICKING)
                .status(WarehouseTaskStatus.COMPLETED)
                .assignedUser(UserTestData.entityBuilder().id(1L).build())
                .product(ProductTestData.entityBuilder().id(1L).build())
                .plannedQuantity(10)
                .confirmedQuantity(10)
                .sourceLocation(LocationTestData.entityBuilder().id(1L).code("PICK-01").build())
                .targetLocation(LocationTestData.entityBuilder().id(2L).code("DSPS-01").build())
                .order(OrderTestData.entityBuilder().id(1L).build())
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .completedAt(COMPLETED_AT);
    }

    public static WarehouseTaskResponse response() {
        return new WarehouseTaskResponse(1L, "TASK-00001", WarehouseTaskType.PICKING,
                WarehouseTaskStatus.COMPLETED, "username", "BRP-ENG-001", 10,
                10, "PICK-01", "DSPS-01", "ORD-00001",
                CREATED_AT, UPDATED_AT, COMPLETED_AT);
    }
}
