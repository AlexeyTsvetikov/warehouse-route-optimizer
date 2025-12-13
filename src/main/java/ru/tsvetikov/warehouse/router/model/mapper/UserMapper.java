//package ru.tsvetikov.warehouse.router.model.mapper;
//
//import org.mapstruct.*;
//import ru.tsvetikov.warehouse.router.model.db.entity.User;
//import ru.tsvetikov.warehouse.router.model.dto.request.UserRequest;
//import ru.tsvetikov.warehouse.router.model.dto.response.UserResponse;
//
//@Mapper(componentModel = "spring")
//public interface UserMapper {
//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "active", constant = "true")
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "tasks", ignore = true)
//    @Mapping(target = "passwordHash", source = "password")
//    User toEntity(UserRequest userRequest);
//
//    UserResponse toResponseDto(User user);
//
//    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "active", ignore = true)
//    @Mapping(target = "tasks", ignore = true)
//    @Mapping(target = "passwordHash", source = "password")
//    void updateEntityFromDto(UserRequest userRequest, @MappingTarget User user);
//}
