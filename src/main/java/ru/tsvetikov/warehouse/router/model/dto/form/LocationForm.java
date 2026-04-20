package ru.tsvetikov.warehouse.router.model.dto.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.tsvetikov.warehouse.router.model.enums.LocationType;

@Getter
@Setter
@NoArgsConstructor
public class LocationForm {

    @NotBlank(message = "Название обязательно")
    @Size(max = 100)
    private String code;

    @NotNull
    private LocationType type;

    @Positive
    private Double width;

    @Positive
    private Double height;

    @Positive
    private Double depth;

    @Positive
    private Double maxWeight;

    @Min(0)
    private Double coordX;

    @Min(0)
    private Double coordY;

    @Size(max = 500, message = "Описание не должно превышать 500 символов")
    private String description;

    private Boolean isActive = true;
}