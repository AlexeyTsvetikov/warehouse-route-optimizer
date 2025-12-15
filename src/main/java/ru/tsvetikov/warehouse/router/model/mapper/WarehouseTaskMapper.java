package ru.tsvetikov.warehouse.router.model.mapper;

import org.mapstruct.*;
import ru.tsvetikov.warehouse.router.model.db.entity.WarehouseTask;
import ru.tsvetikov.warehouse.router.model.dto.response.WarehouseTaskResponse;

@Mapper(componentModel = "spring")
public interface WarehouseTaskMapper {
    @Mapping(target = "assignedUsername", source = "assignedUser.username")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "sourceLocationCode", source = "sourceLocation.code")
    @Mapping(target = "targetLocationCode", source = "targetLocation.code")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    WarehouseTaskResponse toResponseDto(WarehouseTask task);
}