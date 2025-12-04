package ru.tsvetikov.warehouse.router.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tsvetikov.warehouse.router.model.dto.request.StorageRackRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.StorageRackResponse;
import ru.tsvetikov.warehouse.router.service.StorageRackService;

import java.util.List;

@RestController
@RequestMapping("/api/racks")
@RequiredArgsConstructor
public class StorageRackController {
    private final StorageRackService storageRackService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StorageRackResponse create(@RequestBody StorageRackRequest request) {
        return storageRackService.create(request);
    }

    @GetMapping("/{id}")
    public StorageRackResponse getById(@PathVariable Long id) {
        return storageRackService.getById(id);
    }

    @GetMapping
    public List<StorageRackResponse> getAll() {
        return storageRackService.getAll();
    }

    @GetMapping("/zone/{zone}")
    public List<StorageRackResponse> getByZone(@PathVariable String zone) {
        return storageRackService.getByZone(zone);
    }

    @PutMapping("/{id}")
    public StorageRackResponse update(@PathVariable Long id, @RequestBody StorageRackRequest request) {
        return storageRackService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        storageRackService.delete(id);
    }
}
