package ru.tsvetikov.warehouse.router.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tsvetikov.warehouse.router.model.dto.request.UserRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.UserResponse;
import ru.tsvetikov.warehouse.router.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @GetMapping("/id/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAll();
    }

    @GetMapping("/username/{username}")
    public UserResponse getByUsername(@PathVariable String username) {
        return userService.getByUsername(username);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @PatchMapping("/{id}/location")
    public void updateUserLocation(@PathVariable Long id, @RequestParam Double x, @RequestParam Double y) {
        userService.updateUserLocation(id, x, y);
    }
}
