package ru.tsvetikov.warehouse.router.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.dto.request.ProductRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.ProductResponse;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "volume", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "orderDetails", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "priority", source = "priority", defaultValue = "NORMAL")
    Product toEntity(ProductRequest productRequest);

    ProductResponse toResponseDto(Product product);
}