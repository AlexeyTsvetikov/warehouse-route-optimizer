package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;
import ru.tsvetikov.warehouse.router.model.dto.response.RouteResponse;
import ru.tsvetikov.warehouse.router.model.dto.response.WarehouseTaskResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final LocationService locationService;

    public RouteResponse calculateRoute(List<WarehouseTaskResponse> tasks, double startX, double startY) {
        if (tasks.isEmpty()) {
            return new RouteResponse(List.of(), List.of(), 0, 0);
        }

        double fifoDistance = calculateTotalDistance(tasks, startX, startY);

        List<WarehouseTaskResponse> tspOrder = new ArrayList<>(tasks);
        tspOrder.sort(Comparator.comparingDouble(task -> {
            if (task.sourceLocationCode() == null) return Double.MAX_VALUE;
            Location loc = locationService.getByCode(task.sourceLocationCode());
            return Math.abs(loc.getCoordX() - startX) + Math.abs(loc.getCoordY() - startY);
        }));
        double tspDistance = calculateTotalDistance(tspOrder, startX, startY);

        log.info("Route calculated: FIFO={}m, TSP={}m, Saved={}m ({}%)",
                fifoDistance, tspDistance,
                Math.round((fifoDistance - tspDistance) * 100.0) / 100.0,
                fifoDistance > 0 ? (int) Math.round((1 - tspDistance / fifoDistance) * 100) : 0);

        return new RouteResponse(tasks, tspOrder, fifoDistance, tspDistance);
    }

    private double calculateTotalDistance(List<WarehouseTaskResponse> tasks, double startX, double startY) {
        double total = 0;
        double prevX = startX, prevY = startY;

        for (WarehouseTaskResponse task : tasks) {
            if (task.sourceLocationCode() != null) {
                Location loc = locationService.getByCode(task.sourceLocationCode());
                total += Math.abs(loc.getCoordX() - prevX) + Math.abs(loc.getCoordY() - prevY);
                prevX = loc.getCoordX();
                prevY = loc.getCoordY();
            }
        }

        return Math.round(total * 100.0) / 100.0;
    }
}