package ru.tsvetikov.warehouse.router.model.mapper;

import org.mapstruct.*;
import ru.tsvetikov.warehouse.router.model.db.entity.RackCell;
import ru.tsvetikov.warehouse.router.model.dto.request.RackCellRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.RackCellResponse;
import ru.tsvetikov.warehouse.router.model.dto.response.RackCellSimpleResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RackCellMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storageRack", ignore = true)
    @Mapping(target = "sourceTasks", ignore = true)
    @Mapping(target = "targetTasks", ignore = true)
    RackCell toEntity(RackCellRequest request);

    @Mapping(target = "storageRackId", source = "storageRack.id")
    RackCellResponse toResponseDto(RackCell rackCell);

    List<RackCellResponse> toResponseDtoList(List<RackCell> rackCells);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storageRack", ignore = true)
    @Mapping(target = "sourceTasks", ignore = true)
    @Mapping(target = "targetTasks", ignore = true)
    void updateEntityFromDto(RackCellRequest request, @MappingTarget RackCell rackCell);

    RackCellSimpleResponse toSimpleResponseDto(RackCell rackCell);
}
