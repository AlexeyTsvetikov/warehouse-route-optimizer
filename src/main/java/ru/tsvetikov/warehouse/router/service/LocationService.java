//package ru.tsvetikov.warehouse.router.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
//import ru.tsvetikov.warehouse.router.model.db.entity.Location;
//import ru.tsvetikov.warehouse.router.model.db.repository.LocationRepository;
//import ru.tsvetikov.warehouse.router.model.dto.request.LocationRequest;
//import ru.tsvetikov.warehouse.router.model.dto.response.LocationResponse;
//import ru.tsvetikov.warehouse.router.model.mapper.LocationMapper;
//
//import java.util.List;
//import java.util.stream.Collectors;

//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class LocationService {
//    private final LocationRepository locationRepository;
//    private final LocationMapper locationMapper;
//
//    @Transactional
//    public LocationResponse create(LocationRequest request) {
//
//        if (locationRepository.existsByCellCode(request.())) {
//            throw new CommonBackendException(
//                    "Cell with code already exists: " + request.cellCode(), HttpStatus.CONFLICT);
//        }
//
//        Rack rack = locationRepository.findById(request.storageRackId())
//                .orElseThrow(() -> new CommonBackendException(
//                        "Storage rack not found with id: " + request.storageRackId(), HttpStatus.NOT_FOUND));
//
//        Location location = locationMapper.toEntity(request);
//        location.setRack(rack);
//
//        validateCellData(location);
//
//        Location saved = locationRepository.save(location);
//        return locationMapper.toResponseDto(saved);
//    }
//
//    @Transactional(readOnly = true)
//    public LocationResponse getById(Long id) {
//        Location location = locationRepository.findById(id)
//                .orElseThrow(() -> new CommonBackendException(
//                        "Rack cell not found with id: " + id, HttpStatus.NOT_FOUND));
//        return locationMapper.toResponseDto(location);
//    }
//
//    @Transactional(readOnly = true)
//    public List<LocationResponse> getAll() {
//        return locationMapper.toResponseDtoList(locationRepository.findAll());
//    }
//
//    @Transactional(readOnly = true)
//    public List<RackCellSimpleResponse> getCellsByRackId(Long rackId) {
//        return locationRepository.findByStorageRackId(rackId).stream()
//                .map(locationMapper::toSimpleResponseDto)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional(readOnly = true)
//    public List<RackCellSimpleResponse> getFreeCells() {
//        return locationRepository.findByOccupiedFalse().stream()
//                .map(locationMapper::toSimpleResponseDto)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional
//    public void delete(Long id) {
//        if (!locationRepository.existsById(id)) {
//            throw new CommonBackendException(
//                    "Rack cell not found with id: " + id, HttpStatus.NOT_FOUND);
//        }
//        locationRepository.deleteById(id);
//    }
//
//    @Transactional
//    public LocationResponse update(Long id, LocationRequest request) {
//        Location existing = locationRepository.findById(id)
//                .orElseThrow(() -> new CommonBackendException(
//                        "Rack cell not found with id: " + id, HttpStatus.NOT_FOUND));
//
//        if (request.cellCode() != null && !request.cellCode().equals(existing.getCellCode())) {
//            if (locationRepository.existsByCellCode(request.cellCode())) {
//                throw new CommonBackendException(
//                        "Cell with code already exists: " + request.cellCode(), HttpStatus.CONFLICT);
//            }
//        }
//
//        if (request.storageRackId() != null && !request.storageRackId().equals(existing.getRack().getId())) {
//            Rack newRack = locationRepository.findById(request.storageRackId())
//                    .orElseThrow(() -> new CommonBackendException(
//                            "Storage rack not found with id: " + request.storageRackId(), HttpStatus.NOT_FOUND));
//            existing.setRack(newRack);
//        }
//
//        locationMapper.updateEntityFromDto(request, existing);
//        validateCellData(existing);
//
//        Location updated = locationRepository.save(existing);
//        return locationMapper.toResponseDto(updated);
//    }
//
//    private void validateCellData(Location location) {
//        // Проверка объема
//        if (location.getCurrentVolume() > location.getMaxVolume()) {
//            throw new CommonBackendException(
//                    "Current volume cannot exceed max volume",
//                    HttpStatus.BAD_REQUEST
//            );
//        }
//
//        // Проверка координат (добавить логику склада)
//        if (location.getCoordX() < 0 || location.getCoordY() < 0) {
//            throw new CommonBackendException("Coordinates cannot be negative", HttpStatus.BAD_REQUEST);
//        }
//    }
//}
