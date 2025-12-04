package ru.tsvetikov.warehouse.router.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tsvetikov.warehouse.router.model.dto.request.RackCellRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.RackCellResponse;
import ru.tsvetikov.warehouse.router.model.dto.response.RackCellSimpleResponse;
import ru.tsvetikov.warehouse.router.service.RackCellService;

import java.util.List;

@RestController
@RequestMapping("/api/cells")
@RequiredArgsConstructor
public class RackCellController {
    private final RackCellService rackCellService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RackCellResponse create(@RequestBody RackCellRequest request) {
        return rackCellService.create(request);
    }

    @GetMapping("/{id}")
    public RackCellResponse getById(@PathVariable Long id) {
        return rackCellService.getById(id);
    }

    @GetMapping
    public List<RackCellResponse> getAll() {
        return rackCellService.getAll();
    }

    @GetMapping("/rack/{rackId}")
    public List<RackCellSimpleResponse> getCellsByRackId(@PathVariable Long rackId) {
        return rackCellService.getCellsByRackId(rackId);
    }

    @GetMapping("/free")
    public List<RackCellSimpleResponse> getFreeCells() {
        return rackCellService.getFreeCells();
    }

    @PutMapping("/{id}")
    public RackCellResponse update(@PathVariable Long id, @RequestBody RackCellRequest request) {
        return rackCellService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        rackCellService.delete(id);
    }
}