//package ru.tsvetikov.warehouse.router.model.mapper;
//
//import org.mapstruct.Mapper;
//import org.mapstruct.Mapping;
//import ru.tsvetikov.warehouse.router.model.db.entity.OrderItem;
//import ru.tsvetikov.warehouse.router.model.dto.response.OrderItemResponse;
//
//@Mapper(componentModel = "spring")
//public interface OrderItemMapper {
//    @Mapping(source = "order.orderNumber", target = "orderNumber")
//    @Mapping(source = "product.trackingNumber", target = "trackingNumber")
//    OrderItemResponse toResponseDto(OrderItem orderItem);
//}
