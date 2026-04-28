package ru.tsvetikov.warehouse.router.model.mapper;

import org.mapstruct.*;
import ru.tsvetikov.warehouse.router.model.db.entity.OrderItem;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderItemRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderItemResponse;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "collectedQuantity", ignore = true)
    OrderItem toEntity(OrderItemRequest request);

    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "isFullyCollected", expression = "java(orderItem.isFullyCollected())")
    OrderItemResponse toResponseDto(OrderItem orderItem);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "collectedQuantity", ignore = true)
    void updateEntityFromDto(OrderItemRequest request, @MappingTarget OrderItem orderItem);
}