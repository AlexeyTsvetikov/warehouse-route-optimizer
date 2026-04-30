package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseTaskManager {

    private final WarehouseTaskService warehouseTaskService;
    private final StockService stockService;
    private final LocationService locationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createSingleTask(WarehouseTaskRequest request) {
        warehouseTaskService.create(request);
    }

    public String findLocationForProduct(String productSku) {
        return stockService.findFirstAvailableLocationCodeForProduct(productSku)
                .orElse(null);
    }

    public String findDefaultReceivingLocation() {
        return locationService.findFirstReceivingLocationCode()
                .orElse(null);
    }
}