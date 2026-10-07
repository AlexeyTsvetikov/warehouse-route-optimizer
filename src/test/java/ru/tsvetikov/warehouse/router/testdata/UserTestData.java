package ru.tsvetikov.warehouse.router.testdata;

import ru.tsvetikov.warehouse.router.model.db.entity.User;
import ru.tsvetikov.warehouse.router.model.dto.request.UserCreateRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.UserUpdateRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.UserResponse;
import ru.tsvetikov.warehouse.router.model.enums.Role;

public final class UserTestData {

    private UserTestData() {
    }

    public static UserCreateRequest createRequest(String username) {
        return new UserCreateRequest(username, "password", "Firstname",
                "Lastname", Role.PICKER);
    }

    public static UserCreateRequest createRequest() {
        return new UserCreateRequest("username", "password", "Firstname",
                "Lastname", Role.PICKER);
    }

    public static UserUpdateRequest updateRequest() {
        return new UserUpdateRequest("username", "Firstname",
                "Lastname", Role.PICKER);
    }

    public static UserUpdateRequest updateRequest(String username) {
        return new UserUpdateRequest(username, "Firstname",
                "Lastname", Role.PICKER);
    }

    public static User.UserBuilder entityBuilder() {
        return User.builder()
                .username("username")
                .passwordHash("hash")
                .firstName("Firstname")
                .lastName("Lastname")
                .role(Role.PICKER);
    }


    public static UserResponse response() {
        return new UserResponse(1L, "username", "Firstname", "Lastname", Role.PICKER,
                0.0, 0.0);
    }
}
