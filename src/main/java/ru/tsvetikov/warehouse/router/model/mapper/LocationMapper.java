//package ru.tsvetikov.warehouse.router.model.mapper;
//
//import org.mapstruct.*;
//import ru.tsvetikov.warehouse.router.model.db.entity.Location;
//import ru.tsvetikov.warehouse.router.model.dto.request.LocationRequest;
//import ru.tsvetikov.warehouse.router.model.dto.response.LocationResponse;
//
//import java.util.List;
//
//@Mapper(componentModel = "spring")
//public interface LocationMapper {
//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "storageRack", ignore = true)
//    @Mapping(target = "sourceTasks", ignore = true)
//    @Mapping(target = "targetTasks", ignore = true)
//    Location toEntity(LocationRequest request);
//
//    @Mapping(target = "storageRackId", source = "storageRack.id")
//    LocationResponse toResponseDto(Location location);
//
//    List<LocationResponse> toResponseDtoList(List<Location> locations);
//
//    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "storageRack", ignore = true)
//    @Mapping(target = "sourceTasks", ignore = true)
//    @Mapping(target = "targetTasks", ignore = true)
//    void updateEntityFromDto(LocationRequest request, @MappingTarget Location location);
//
//    LocationResponse toSimpleResponseDto(Location location);
//}
