package ru.tsvetikov.warehouse.router.testdata;

import ru.tsvetikov.warehouse.router.model.db.entity.Location;
import ru.tsvetikov.warehouse.router.model.dto.form.LocationForm;
import ru.tsvetikov.warehouse.router.model.dto.request.LocationRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.LocationResponse;
import ru.tsvetikov.warehouse.router.model.enums.LocationType;

public final class LocationTestData {

    private LocationTestData() {
    }

    public static LocationRequest request() {
        return new LocationRequest("recv-01", LocationType.RECEIVING,
                1.0, 2.0, 3.0, 100.0, 10.0, 15.0, "Receiving");
    }

    public static LocationForm form(String code) {
        LocationForm form = new LocationForm();
        form.setCode(code);
        form.setType(LocationType.RECEIVING);
        form.setWidth(1.0);
        form.setHeight(2.0);
        form.setDepth(3.0);
        form.setMaxWeight(100.0);
        form.setCoordX(10.0);
        form.setCoordY(15.0);
        form.setDescription("Receiving");
        form.setIsActive(true);
        return form;
    }

    public static LocationRequest request(String code) {
        return new LocationRequest(code, LocationType.RECEIVING,
                1.0, 2.0, 3.0, 100.0, 10.0, 15.0, "Receiving");
    }

    public static Location.LocationBuilder entityBuilder() {
        return Location.builder()
                .type(LocationType.RECEIVING)
                .width(1.0).height(2.0).depth(3.0).maxWeight(100.0)
                .coordX(10.0).coordY(15.0)
                .description("Receiving");
    }

    public static LocationResponse response() {
        return new LocationResponse(1L, "RECV-01", LocationType.RECEIVING,
                1.0, 2.0, 3.0, 6.0, 100.0, 10.0, 15.0, "Receiving", true);
    }
}
