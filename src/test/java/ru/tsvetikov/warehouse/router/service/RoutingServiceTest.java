package ru.tsvetikov.warehouse.router.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;
import ru.tsvetikov.warehouse.router.model.dto.response.RouteResponse;
import ru.tsvetikov.warehouse.router.model.dto.response.WarehouseTaskResponse;
import ru.tsvetikov.warehouse.router.testdata.LocationTestData;
import ru.tsvetikov.warehouse.router.testdata.WarehouseTaskTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingServiceTest {

    @Mock
    private LocationService locationService;

    @InjectMocks
    private RoutingService routingService;

    @Test
    void shouldReturnEmptyRouteWhenNoTasks() {
        RouteResponse result = routingService.calculateCombinedRoute(List.of(), 20.0, 40.0);

        assertThat(result.fifoOrder()).isEmpty();
        assertThat(result.tspOrder()).isEmpty();
        assertThat(result.fifoDistance()).isEqualTo(0);
        assertThat(result.tspDistance()).isEqualTo(0);
    }

    @Test
    void shouldReturnSameDistanceForOneWarehouseTask() {
        WarehouseTaskResponse response = WarehouseTaskTestData.response().toBuilder()
                .sourceLocationCode("RECV-01").targetLocationCode("RECV-04").build();
        Location location = LocationTestData.entityBuilder().id(1L).code("RECV-01").build();

        when(locationService.getByCode(anyString())).thenReturn(location);

        RouteResponse result = routingService.calculateCombinedRoute(List.of(response), 20.0, 40.0);

        assertThat(result.fifoOrder()).containsExactly(response);
        assertThat(result.tspOrder()).containsExactly(response);
        assertThat(result.fifoDistance()).isEqualTo(35.0);
        assertThat(result.tspDistance()).isEqualTo(35.0);
    }

    @Test
    void shouldReorderTasksWhenTspFindsShorterRoute() {
        WarehouseTaskResponse task1 = WarehouseTaskTestData.response().toBuilder()
                .id(1L).sourceLocationCode("A").targetLocationCode(null).build();
        WarehouseTaskResponse task2 = WarehouseTaskTestData.response().toBuilder()
                .id(2L).sourceLocationCode("B").targetLocationCode(null).build();

        Location locA = LocationTestData.entityBuilder().id(1L).code("A").coordX(100.0).coordY(100.0).build();
        Location locB = LocationTestData.entityBuilder().id(2L).code("B").coordX(10.0).coordY(10.0).build();

        when(locationService.getByCode("A")).thenReturn(locA);
        when(locationService.getByCode("B")).thenReturn(locB);

        RouteResponse result = routingService.calculateCombinedRoute(List.of(task1, task2), 0.0, 0.0);

        assertThat(result.fifoOrder()).containsExactly(task1, task2);
        assertThat(result.tspOrder()).containsExactly(task2, task1);
        assertThat(result.fifoDistance()).isEqualTo(380.0);
        assertThat(result.tspDistance()).isEqualTo(200.0);
    }

    @Test
    void shouldSkipTaskWithoutLocationInDistanceCalculation() {
        WarehouseTaskResponse task1 = WarehouseTaskTestData.response().toBuilder()
                .id(1L).sourceLocationCode("A").targetLocationCode(null).build();
        WarehouseTaskResponse task2 = WarehouseTaskTestData.response().toBuilder()
                .id(2L).sourceLocationCode(null).targetLocationCode(null).build();

        Location locA = LocationTestData.entityBuilder().id(1L).code("A").coordX(100.0).coordY(100.0).build();

        when(locationService.getByCode("A")).thenReturn(locA);

        RouteResponse result = routingService.calculateCombinedRoute(List.of(task1, task2), 0.0, 0.0);

        assertThat(result.fifoOrder()).containsExactly(task1, task2);
        assertThat(result.tspOrder()).containsExactly(task1, task2);
        assertThat(result.fifoDistance()).isEqualTo(200.0);
        assertThat(result.tspDistance()).isEqualTo(200.0);
    }

    @Test
    void shouldReturnRouteWhenSourceLocationIsNull() {
        WarehouseTaskResponse task1 = WarehouseTaskTestData.response().toBuilder()
                .id(1L).sourceLocationCode(null).targetLocationCode("A").build();
        WarehouseTaskResponse task2 = WarehouseTaskTestData.response().toBuilder()
                .id(2L).sourceLocationCode(null).targetLocationCode("B").build();

        Location locA = LocationTestData.entityBuilder().id(1L).code("A").coordX(100.0).coordY(100.0).build();
        Location locB = LocationTestData.entityBuilder().id(2L).code("B").coordX(10.0).coordY(10.0).build();

        when(locationService.getByCode("A")).thenReturn(locA);
        when(locationService.getByCode("B")).thenReturn(locB);

        RouteResponse result = routingService.calculateCombinedRoute(List.of(task1, task2), 0.0, 0.0);

        assertThat(result.fifoOrder()).containsExactly(task1, task2);
        assertThat(result.tspOrder()).containsExactly(task2, task1);
        assertThat(result.fifoDistance()).isEqualTo(380.0);
        assertThat(result.tspDistance()).isEqualTo(200.0);
    }
}