package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.RackCell;
import ru.tsvetikov.warehouse.router.model.db.entity.StorageRack;
import ru.tsvetikov.warehouse.router.model.db.repository.RackCellRepository;
import ru.tsvetikov.warehouse.router.model.db.repository.StorageRackRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.RackCellRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.RackCellResponse;
import ru.tsvetikov.warehouse.router.model.dto.response.RackCellSimpleResponse;
import ru.tsvetikov.warehouse.router.model.mapper.RackCellMapper;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RackCellService {
    private final RackCellRepository rackCellRepository;
    private final StorageRackRepository storageRackRepository;
    private final RackCellMapper rackCellMapper;

    @Transactional
    public RackCellResponse create(RackCellRequest request) {

        if (rackCellRepository.existsByCellCode(request.cellCode())) {
            throw new CommonBackendException(
                    "Cell with code already exists: " + request.cellCode(), HttpStatus.CONFLICT);
        }

        StorageRack rack = storageRackRepository.findById(request.storageRackId())
                .orElseThrow(() -> new CommonBackendException(
                        "Storage rack not found with id: " + request.storageRackId(), HttpStatus.NOT_FOUND));

        RackCell rackCell = rackCellMapper.toEntity(request);
        rackCell.setStorageRack(rack);

        validateCellData(rackCell);

        RackCell saved = rackCellRepository.save(rackCell);
        return rackCellMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public RackCellResponse getById(Long id) {
        RackCell rackCell = rackCellRepository.findById(id)
                .orElseThrow(() -> new CommonBackendException(
                        "Rack cell not found with id: " + id, HttpStatus.NOT_FOUND));
        return rackCellMapper.toResponseDto(rackCell);
    }

    @Transactional(readOnly = true)
    public List<RackCellResponse> getAll() {
        return rackCellMapper.toResponseDtoList(rackCellRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<RackCellSimpleResponse> getCellsByRackId(Long rackId) {
        return rackCellRepository.findByStorageRackId(rackId).stream()
                .map(rackCellMapper::toSimpleResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RackCellSimpleResponse> getFreeCells() {
        return rackCellRepository.findByOccupiedFalse().stream()
                .map(rackCellMapper::toSimpleResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        if (!rackCellRepository.existsById(id)) {
            throw new CommonBackendException(
                    "Rack cell not found with id: " + id, HttpStatus.NOT_FOUND);
        }
        rackCellRepository.deleteById(id);
    }

    @Transactional
    public RackCellResponse update(Long id, RackCellRequest request) {
        RackCell existing = rackCellRepository.findById(id)
                .orElseThrow(() -> new CommonBackendException(
                        "Rack cell not found with id: " + id, HttpStatus.NOT_FOUND));

        if (request.cellCode() != null && !request.cellCode().equals(existing.getCellCode())) {
            if (rackCellRepository.existsByCellCode(request.cellCode())) {
                throw new CommonBackendException(
                        "Cell with code already exists: " + request.cellCode(), HttpStatus.CONFLICT);
            }
        }

        if (request.storageRackId() != null && !request.storageRackId().equals(existing.getStorageRack().getId())) {
            StorageRack newRack = storageRackRepository.findById(request.storageRackId())
                    .orElseThrow(() -> new CommonBackendException(
                            "Storage rack not found with id: " + request.storageRackId(), HttpStatus.NOT_FOUND));
            existing.setStorageRack(newRack);
        }

        rackCellMapper.updateEntityFromDto(request, existing);
        validateCellData(existing);

        RackCell updated = rackCellRepository.save(existing);
        return rackCellMapper.toResponseDto(updated);
    }

    private void validateCellData(RackCell rackCell) {
        // Проверка объема
        if (rackCell.getCurrentVolume() > rackCell.getMaxVolume()) {
            throw new CommonBackendException(
                    "Current volume cannot exceed max volume",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Проверка координат (добавить логику склада)
        if (rackCell.getCoordX() < 0 || rackCell.getCoordY() < 0) {
            throw new CommonBackendException("Coordinates cannot be negative", HttpStatus.BAD_REQUEST);
        }
    }
}
