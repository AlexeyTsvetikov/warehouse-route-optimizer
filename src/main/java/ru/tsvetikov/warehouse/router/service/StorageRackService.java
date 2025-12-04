package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.StorageRack;
import ru.tsvetikov.warehouse.router.model.db.repository.StorageRackRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.StorageRackRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.StorageRackResponse;
import ru.tsvetikov.warehouse.router.model.mapper.StorageRackMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageRackService {
    private final StorageRackRepository storageRackRepository;
    private final StorageRackMapper storageRackMapper;

    @Transactional
    public StorageRackResponse create(StorageRackRequest request) {
        StorageRack storageRack = storageRackMapper.toEntity(request);
        StorageRack saved = storageRackRepository.save(storageRack);
        return storageRackMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public StorageRackResponse getById(Long id) {
        StorageRack storageRack = storageRackRepository.findById(id)
                .orElseThrow(() -> new CommonBackendException(
                        "Storage rack not found with id: " + id, HttpStatus.NOT_FOUND));
        return storageRackMapper.toResponseDto(storageRack);
    }

    @Transactional(readOnly = true)
    public List<StorageRackResponse> getAll() {
        return storageRackMapper.toResponseDtoList(storageRackRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<StorageRackResponse> getByZone(String zone) {
        return storageRackMapper.toResponseDtoList(storageRackRepository.findByZone(zone));
    }

    @Transactional
    public void delete(Long id) {
        if (!storageRackRepository.existsById(id)) {
            throw new CommonBackendException("Storage rack not found with id: " + id, HttpStatus.NOT_FOUND);
        }
        storageRackRepository.deleteById(id);
    }

    @Transactional
    public StorageRackResponse update(Long id, StorageRackRequest request) {
        StorageRack existing = storageRackRepository.findById(id)
                .orElseThrow(() -> new CommonBackendException(
                        "Storage rack not found with id: " + id, HttpStatus.NOT_FOUND));

        storageRackMapper.updateEntityFromDto(request, existing);
        StorageRack updated = storageRackRepository.save(existing);
        return storageRackMapper.toResponseDto(updated);
    }
}