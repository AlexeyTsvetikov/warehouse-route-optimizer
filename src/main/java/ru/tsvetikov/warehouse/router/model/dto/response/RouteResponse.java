package ru.tsvetikov.warehouse.router.model.dto.response;

import java.util.List;

public record RouteResponse(
        List<WarehouseTaskResponse> fifoOrder,
        List<WarehouseTaskResponse> tspOrder,
        double fifoDistance,
        double tspDistance
) {
    public double getSavedDistance() {
        return Math.round((fifoDistance - tspDistance) * 100.0) / 100.0;
    }

    public int getSavedPercent() {
        return fifoDistance > 0 ? (int) Math.round((1 - tspDistance / fifoDistance) * 100) : 0;
    }
}
