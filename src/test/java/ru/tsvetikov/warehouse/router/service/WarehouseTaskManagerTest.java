package ru.tsvetikov.warehouse.router.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;
import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;
import ru.tsvetikov.warehouse.router.model.enums.LocationType;
import ru.tsvetikov.warehouse.router.testdata.LocationTestData;
import ru.tsvetikov.warehouse.router.testdata.WarehouseTaskTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseTaskManagerTest {

    @Mock
    private WarehouseTaskService warehouseTaskService;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private WarehouseTaskManager warehouseTaskManager;

    @Test
    void createSingleTask() {
        WarehouseTaskRequest request = WarehouseTaskTestData.request();
        warehouseTaskManager.createSingleTask(request);

        verify(warehouseTaskService).create(request);
    }

    @Test
    void shouldReturnNearestReceivingLocation() {
        Location far = LocationTestData.entityBuilder()
                .id(1L).code("FAR").coordX(100.0).coordY(100.0).build();
        Location near = LocationTestData.entityBuilder()
                .id(2L).code("NEAR").coordX(2.0).coordY(3.0).build();

        when(locationService.findByType(LocationType.RECEIVING))
                .thenReturn(List.of(far, near));

        String result = warehouseTaskManager.findNearestReceivingLocation(1.0, 2.0);

        assertThat(result).isEqualTo("NEAR");
    }

    @Test
    void shouldReturnNullWhenNoReceivingLocations() {
        when(locationService.findByType(LocationType.RECEIVING)).thenReturn(List.of());

        String result = warehouseTaskManager.findNearestReceivingLocation(1.0, 2.0);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenAllLocationsInactive() {
        Location inactive = LocationTestData.entityBuilder()
                .id(1L).code("OFF").isActive(false).build();

        when(locationService.findByType(LocationType.RECEIVING))
                .thenReturn(List.of(inactive));

        String result = warehouseTaskManager.findNearestReceivingLocation(1.0, 2.0);

        assertThat(result).isNull();
    }

    @Test
    void shouldIgnoreInactiveLocations() {
        Location inactiveClose = LocationTestData.entityBuilder()
                .id(1L).code("OFF").isActive(false).coordX(1.0).coordY(2.0).build();
        Location activeFar = LocationTestData.entityBuilder()
                .id(2L).code("ON").isActive(true).coordX(50.0).coordY(50.0).build();

        when(locationService.findByType(LocationType.RECEIVING))
                .thenReturn(List.of(inactiveClose, activeFar));

        String result = warehouseTaskManager.findNearestReceivingLocation(1.0, 2.0);

        assertThat(result).isEqualTo("ON");
    }
}