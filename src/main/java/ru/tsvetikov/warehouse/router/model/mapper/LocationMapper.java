package ru.tsvetikov.warehouse.router.model.mapper;

import org.mapstruct.*;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;
import ru.tsvetikov.warehouse.router.model.dto.form.LocationForm;
import ru.tsvetikov.warehouse.router.model.dto.request.LocationRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.LocationResponse;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "stocks", ignore = true)
    @Mapping(target = "volume", ignore = true)
    Location toEntity(LocationRequest request);

    LocationResponse toResponseDto(Location location);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "stocks", ignore = true)
    @Mapping(target = "volume", ignore = true)
    void updateEntityFromDto(LocationRequest request, @MappingTarget Location location);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "stocks", ignore = true)
    @Mapping(target = "volume", ignore = true)
    void updateEntityFromForm(LocationForm form, @MappingTarget Location location);
}
