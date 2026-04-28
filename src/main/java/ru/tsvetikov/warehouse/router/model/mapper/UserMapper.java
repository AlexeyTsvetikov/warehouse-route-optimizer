package ru.tsvetikov.warehouse.router.model.mapper;

import org.mapstruct.*;
import ru.tsvetikov.warehouse.router.model.db.entity.User;
import ru.tsvetikov.warehouse.router.model.dto.request.UserCreateRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.UserUpdateRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.UserResponse;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "warehouseTasks", ignore = true)
    @Mapping(target = "lastKnownX", ignore = true)
    @Mapping(target = "lastKnownY", ignore = true)
    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "username", ignore = true)
    User toEntity(UserCreateRequest userRequest);

    UserResponse toResponseDto(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "warehouseTasks", ignore = true)
    @Mapping(target = "lastKnownX", ignore = true)
    @Mapping(target = "lastKnownY", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "username", ignore = true)
    void updateEntityFromDto(UserUpdateRequest userRequest, @MappingTarget User user);
}
