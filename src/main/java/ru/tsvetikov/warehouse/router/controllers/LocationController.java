//package ru.tsvetikov.warehouse.router.controllers;
//
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.*;
//import ru.tsvetikov.warehouse.router.model.dto.request.LocationRequest;
//import ru.tsvetikov.warehouse.router.model.dto.response.LocationResponse;
//import ru.tsvetikov.warehouse.router.service.LocationService;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/cells")
//@RequiredArgsConstructor
//public class LocationController {
//    private final LocationService locationService;
//
//    @PostMapping
//    @ResponseStatus(HttpStatus.CREATED)
//    public LocationResponse create(@Valid @RequestBody LocationRequest request) {
//        return locationService.create(request);
//    }
//
//    @GetMapping("/{id}")
//    public LocationResponse getById(@PathVariable Long id) {
//        return locationService.getById(id);
//    }
//
//    @GetMapping
//    public List<LocationResponse> getAll() {
//        return locationService.getAll();
//    }
//
//    @GetMapping("/rack/{rackId}")
//    public List<RackCellSimpleResponse> getCellsByRackId(@PathVariable Long rackId) {
//        return locationService.getCellsByRackId(rackId);
//    }
//
//    @GetMapping("/free")
//    public List<RackCellSimpleResponse> getFreeCells() {
//        return locationService.getFreeCells();
//    }
//
//    @PutMapping("/{id}")
//    public LocationResponse update(@PathVariable Long id, @Valid @RequestBody LocationRequest request) {
//        return locationService.update(id, request);
//    }
//
//    @DeleteMapping("/{id}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void delete(@PathVariable Long id) {
//        locationService.delete(id);
//    }
//}