package ru.tsvetikov.warehouse.router.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.tsvetikov.warehouse.router.model.db.entity.OrderDetail;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderDetailResponse;

@Mapper(componentModel = "spring")
public interface OrderDetailMapper {
    @Mapping(source = "order.orderNumber", target = "orderNumber")
    @Mapping(source = "product.trackingNumber", target = "trackingNumber")
    OrderDetailResponse toResponseDto(OrderDetail orderDetail);
}
