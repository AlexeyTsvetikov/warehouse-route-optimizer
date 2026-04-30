package ru.tsvetikov.warehouse.router.event;

import ru.tsvetikov.warehouse.router.model.db.entity.WarehouseTask;

public record TaskCompletedEvent(WarehouseTask task) {}
