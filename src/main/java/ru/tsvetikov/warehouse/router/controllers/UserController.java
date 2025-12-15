package ru.tsvetikov.warehouse.router.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tsvetikov.warehouse.router.model.dto.request.UserRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.UserResponse;
import ru.tsvetikov.warehouse.router.service.UserService;

@Tag(name = "Users", description = "Управление пользователями системы")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "Создать нового пользователя")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody @Valid UserRequest request) {
        return userService.create(request);
    }

    @Operation(summary = "Получить пользователя по ID")
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @Operation(summary = "Получить всех пользователей с пагинацией")
    @GetMapping
    public Page<UserResponse> getAll(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                     @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer perPage,
                                     @RequestParam(defaultValue = "username") String sort,
                                     @RequestParam(defaultValue = "ASC") Sort.Direction order) {
        return userService.getAll(page, perPage, sort, order);
    }

    @Operation(summary = "Обновить пользователя")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @RequestBody @Valid UserRequest request) {
        return userService.update(id, request);
    }

    @Operation(summary = "Удалить пользователя")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @Operation(summary = "Восстановить пользователя")
    @PatchMapping("/{id}/activate")
    public UserResponse activate(@PathVariable Long id) {
        return userService.activate(id);
    }

    @Operation(summary = "Обновить местоположение пользователя")
    @PatchMapping("/{id}/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLocation(@PathVariable Long id, @RequestParam Double x, @RequestParam Double y) {
        userService.updateLocation(id, x, y);
    }
}