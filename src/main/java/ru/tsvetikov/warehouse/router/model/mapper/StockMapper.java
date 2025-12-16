package ru.tsvetikov.warehouse.router.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.tsvetikov.warehouse.router.model.db.entity.Stock;
import ru.tsvetikov.warehouse.router.model.dto.response.StockResponse;

@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "locationCode", source = "location.code")
    @Mapping(target = "availableQuantity", expression = "java(stock.getAvailableQuantity())")
    StockResponse toResponse(Stock stock);
}
