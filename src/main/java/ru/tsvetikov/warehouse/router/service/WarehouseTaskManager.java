package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;
import ru.tsvetikov.warehouse.router.model.db.repository.LocationRepository;
import ru.tsvetikov.warehouse.router.model.db.repository.StockRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;
import ru.tsvetikov.warehouse.router.model.enums.LocationType;

@Service
@RequiredArgsConstructor
public class WarehouseTaskManager {

    private final WarehouseTaskService warehouseTaskService;
    private final StockRepository stockRepository;
    private final LocationRepository locationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createSingleTask(WarehouseTaskRequest request) {
        warehouseTaskService.create(request);
    }

    public String findLocationForProduct(String productSku) {
        return stockRepository.findFirstAvailableLocationForProduct(productSku)
                .map(Location::getCode)
                .orElse(null);
    }

    public String findDefaultReceivingLocation() {
        return locationRepository.findFirstByType(LocationType.RECEIVING)
                .map(Location::getCode)
                .orElse(null);
    }
}