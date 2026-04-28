package ru.tsvetikov.warehouse.router.model.mapper;

import org.mapstruct.*;
import ru.tsvetikov.warehouse.router.model.db.entity.Order;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderResponse;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "status", constant = "NEW")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "warehouseTasks", ignore = true)
    Order toEntity(OrderRequest orderRequest);

    OrderResponse toResponseDto(Order order);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "warehouseTasks", ignore = true)
    void updateEntityFromDto(OrderRequest orderRequest, @MappingTarget Order order);
}
