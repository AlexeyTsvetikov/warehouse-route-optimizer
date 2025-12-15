package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;
import ru.tsvetikov.warehouse.router.model.db.repository.LocationRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.LocationRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.LocationResponse;
import ru.tsvetikov.warehouse.router.model.mapper.LocationMapper;
import ru.tsvetikov.warehouse.router.utils.PaginationUtils;


@Service
@RequiredArgsConstructor
public class LocationService {
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    @Transactional
    public LocationResponse create(LocationRequest request) {
        checkCodeUniqueness(request.code());
        Location location = locationMapper.toEntity(request);
        Location saved = locationRepository.save(location);
        return locationMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public LocationResponse getById(Long id) {
        Location location = findLocationOrThrow(id);
        return locationMapper.toResponseDto(location);
    }

    @Transactional(readOnly = true)
    public Page<LocationResponse> getAll(Integer page, Integer perPage,
                                         String sort, Sort.Direction order) {
        Pageable pageRequest = PaginationUtils.getPageRequest(page, perPage, sort, order);
        return locationRepository.findAllByIsActiveTrue(pageRequest)
                .map(locationMapper::toResponseDto);
    }

    @Transactional
    public LocationResponse update(Long id, LocationRequest request) {
        Location location = findLocationOrThrow(id);

        if (request.code() != null && !request.code().equals(location.getCode())) {
            checkCodeUniqueness(request.code());
        }

        locationMapper.updateEntityFromDto(request, location);
        Location updatedLocation = locationRepository.save(location);
        return locationMapper.toResponseDto(updatedLocation);
    }

    @Transactional
    public void delete(Long id) {
        Location location = findLocationOrThrow(id);

        if (!location.getIsActive()) {
            throw new CommonBackendException("Location is already deleted", HttpStatus.CONFLICT);
        }
        location.setIsActive(false);
        locationRepository.save(location);
    }

    @Transactional
    public LocationResponse activate(Long id) {
        Location location = findLocationOrThrow(id);

        if (location.getIsActive()) {
            throw new CommonBackendException("Location is already active", HttpStatus.CONFLICT);
        }
        location.setIsActive(true);
        return locationMapper.toResponseDto(location);
    }

    private Location findLocationOrThrow(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Location with id: %s not found", id), HttpStatus.NOT_FOUND));
    }

    private void checkCodeUniqueness(String code) {
        if (locationRepository.existsByCode(code)) {
            throw new CommonBackendException(
                    String.format("Location with code already exists: %s", code), HttpStatus.CONFLICT);
        }
    }
}