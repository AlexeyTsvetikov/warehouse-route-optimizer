package ru.tsvetikov.warehouse.router.model.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;

@Getter
@Setter
@NoArgsConstructor
public class WarehouseTaskForm {
    @NotNull
    private WarehouseTaskType type;

    @NotBlank
    private String productSku;

    @NotNull @Positive
    private Integer plannedQuantity;

    private String sourceLocationCode;
    private String targetLocationCode;
    private String assignedUsername;
    private String orderNumber;
    private Integer confirmedQuantity;
}
