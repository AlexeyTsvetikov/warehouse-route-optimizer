package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;
import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;
import ru.tsvetikov.warehouse.router.model.enums.LocationType;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseTaskManager {

    private final WarehouseTaskService warehouseTaskService;
    private final LocationService locationService;

    @Transactional
    public void createSingleTask(WarehouseTaskRequest request) {
        warehouseTaskService.create(request);
    }

    public String findNearestReceivingLocation(double fromX, double fromY) {
        List<Location> receivingLocations = locationService.findByType(LocationType.RECEIVING);

        if (receivingLocations.isEmpty()) {
            return null;
        }

        return receivingLocations.stream()
                .filter(Location::getIsActive)
                .min(Comparator.comparingDouble(loc ->
                        Math.abs(loc.getCoordX() - fromX) + Math.abs(loc.getCoordY() - fromY)))
                .map(Location::getCode)
                .orElse(null);
    }
}