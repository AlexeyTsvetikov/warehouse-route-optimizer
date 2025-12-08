package ru.tsvetikov.warehouse.router.model.mapper;

import org.mapstruct.*;
import ru.tsvetikov.warehouse.router.model.db.entity.Task;
import ru.tsvetikov.warehouse.router.model.dto.response.TaskResponse;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "product.trackingNumber", target = "productTrackingNumber")
    @Mapping(source = "sourceCell.cellCode", target = "sourceCellCode")
    @Mapping(source = "targetCell.cellCode", target = "targetCellCode")
    @Mapping(source = "order.orderNumber", target = "orderNumber")
    TaskResponse toResponseDto(Task task);
}
