package ru.tsvetikov.warehouse.router.model.mapper;

import org.mapstruct.*;
import ru.tsvetikov.warehouse.router.model.db.entity.StorageRack;
import ru.tsvetikov.warehouse.router.model.dto.request.StorageRackRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.StorageRackResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StorageRackMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rackCells", ignore = true)
    StorageRack toEntity(StorageRackRequest request);

    StorageRackResponse toResponseDto(StorageRack storageRack);

    List<StorageRackResponse> toResponseDtoList(List<StorageRack> racks);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rackCells", ignore = true)
    void updateEntityFromDto(StorageRackRequest request, @MappingTarget StorageRack storageRack);
}