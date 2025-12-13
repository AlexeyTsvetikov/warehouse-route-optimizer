//package ru.tsvetikov.warehouse.router.controllers;
//
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.*;
//import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;
//import ru.tsvetikov.warehouse.router.model.dto.response.WarehouseTaskResponse;
//import ru.tsvetikov.warehouse.router.service.WarehouseTaskService;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/tasks")
//@RequiredArgsConstructor
//public class WarehouseTaskController {
//    private final WarehouseTaskService warehouseTaskService;
//
//    @PostMapping
//    @ResponseStatus(HttpStatus.CREATED)
//    public WarehouseTaskResponse create(@Valid @RequestBody WarehouseTaskRequest request) {
//        return warehouseTaskService.create(request);
//    }
//
//    @GetMapping("/{id}")
//    public WarehouseTaskResponse getById(@PathVariable Long id) {
//        return warehouseTaskService.getById(id);
//    }
//
//    @GetMapping
//    public List<WarehouseTaskResponse> getAll() {
//        return warehouseTaskService.getAll();
//    }
//
//    @PatchMapping("/{id}/assign")
//    public WarehouseTaskResponse assignTask(@PathVariable Long id, @RequestParam String username) {
//        return warehouseTaskService.assignTask(id, username);
//    }
//
//    @PatchMapping("/{id}/start")
//    public WarehouseTaskResponse startTask(@PathVariable Long id) {
//        return warehouseTaskService.startTask(id);
//    }
//
//    @PatchMapping("/{id}/complete")
//    public WarehouseTaskResponse completeTask(@PathVariable Long id) {
//        return warehouseTaskService.completeTask(id);
//    }
//
//    @PutMapping("/{id}")
//    public WarehouseTaskResponse update(@PathVariable Long id, @Valid @RequestBody WarehouseTaskRequest request) {
//        return warehouseTaskService.update(id, request);
//    }
//
//    @DeleteMapping("/{id}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void delete(@PathVariable Long id) {
//        warehouseTaskService.delete(id);
//    }
//}
