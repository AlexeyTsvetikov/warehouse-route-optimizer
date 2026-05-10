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

    public RouteResponse calculateCombinedRoute(List<WarehouseTaskResponse> allTasks, double startX, double startY) {
        if (allTasks.isEmpty()) {
            return new RouteResponse(List.of(), List.of(), 0, 0);
        }

        // FIFO — исходный порядок для сравнения
        double fifoDistance = calculateTotalDistance(allTasks, startX, startY);

        // Жадный TSP для всех задач (RECEIVING + PICKING вместе)
        List<WarehouseTaskResponse> remaining = new ArrayList<>(allTasks);
        List<WarehouseTaskResponse> tspOrder = new ArrayList<>();

        double currentX = startX, currentY = startY;

        while (!remaining.isEmpty()) {
            double finalCurrentX = currentX, finalCurrentY = currentY;
            WarehouseTaskResponse nearest = remaining.stream()
                    .min(Comparator.comparingDouble(t -> distanceFromPoint(t, finalCurrentX, finalCurrentY)))
                    .orElseThrow(); // всегда есть элемент

            tspOrder.add(nearest);
            remaining.remove(nearest);

            String locCode = getLocationCode(nearest);
            if (locCode != null) {
                Location loc = locationService.getByCode(locCode);
                currentX = loc.getCoordX();
                currentY = loc.getCoordY();
            }
        }

        double tspDistance = calculateTotalDistance(tspOrder, startX, startY);

        log.info("Combined route: FIFO={}m, TSP={}m, Saved={}m ({}%)",
                fifoDistance, tspDistance,
                Math.round((fifoDistance - tspDistance) * 100.0) / 100.0,
                fifoDistance > 0 ? (int) Math.round((1 - tspDistance / fifoDistance) * 100) : 0);

        return new RouteResponse(allTasks, tspOrder, fifoDistance, tspDistance);
    }

    private double distanceFromPoint(WarehouseTaskResponse task, double x, double y) {
        String locCode = getLocationCode(task);
        if (locCode == null) return Double.MAX_VALUE;
        Location loc = locationService.getByCode(locCode);
        return Math.abs(loc.getCoordX() - x) + Math.abs(loc.getCoordY() - y);
    }

    private String getLocationCode(WarehouseTaskResponse task) {
        if (task.sourceLocationCode() != null) return task.sourceLocationCode();
        if (task.targetLocationCode() != null) return task.targetLocationCode();
        return null;
    }

    private double calculateTotalDistance(List<WarehouseTaskResponse> tasks, double startX, double startY) {
        double total = 0;
        double prevX = startX, prevY = startY;

        for (WarehouseTaskResponse task : tasks) {
            String locCode = getLocationCode(task);
            if (locCode != null) {
                Location loc = locationService.getByCode(locCode);
                total += Math.abs(loc.getCoordX() - prevX) + Math.abs(loc.getCoordY() - prevY);
                prevX = loc.getCoordX();
                prevY = loc.getCoordY();
            }
        }

        return Math.round(total * 100.0) / 100.0;
    }
}