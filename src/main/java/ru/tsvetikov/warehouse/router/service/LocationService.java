package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;
import ru.tsvetikov.warehouse.router.model.db.repository.LocationRepository;
import ru.tsvetikov.warehouse.router.model.dto.form.LocationForm;
import ru.tsvetikov.warehouse.router.model.dto.request.LocationRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.LocationResponse;
import ru.tsvetikov.warehouse.router.model.mapper.LocationMapper;
import ru.tsvetikov.warehouse.router.utils.PaginationUtils;


@Service
@RequiredArgsConstructor
public class LocationService {
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    @Cacheable(value = "locations", key = "#code")
    public Location getByCode(String code) {
        return locationRepository.findByCode(code)
                .orElseThrow(() -> new CommonBackendException("Location not found", HttpStatus.NOT_FOUND));
    }

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

    @Transactional(readOnly = true)
    public Page<LocationResponse> search(String query, int page, int size, String sort, Sort.Direction order) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(order, sort));
        Page<Location> categories = locationRepository.searchActive(query, pageable);
        return categories.map(locationMapper::toResponseDto);
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
    public void updateFromWeb(Long id, LocationForm form) {
        Location location = findLocationOrThrow(id);
        locationMapper.updateEntityFromForm(form, location);
        locationRepository.save(location);
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