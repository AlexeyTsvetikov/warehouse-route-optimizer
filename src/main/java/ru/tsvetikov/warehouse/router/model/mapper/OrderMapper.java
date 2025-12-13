//package ru.tsvetikov.warehouse.router.model.mapper;
//
//import org.mapstruct.*;
//import ru.tsvetikov.warehouse.router.model.db.entity.Order;
//import ru.tsvetikov.warehouse.router.model.dto.request.OrderRequest;
//import ru.tsvetikov.warehouse.router.model.dto.response.OrderResponse;
//
//@Mapper(componentModel = "spring")
//public interface OrderMapper {
//
//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "status", constant = "FORMING")
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "tasks", ignore = true)
//    Order toEntity(OrderRequest orderRequest);
//
//    OrderResponse toResponseDto(Order order);
//
//    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "tasks", ignore = true)
//    void updateEntityFromDto(OrderRequest orderRequest, @MappingTarget Order order);
//
//}
