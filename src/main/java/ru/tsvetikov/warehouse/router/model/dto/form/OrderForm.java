package ru.tsvetikov.warehouse.router.model.dto.form;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import ru.tsvetikov.warehouse.router.model.enums.OrderType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class OrderForm {

    @NotNull
    private OrderType type;

    @Size(max = 100)
    private String customerName;

    @NotBlank @Size(max = 100)
    private String destinationRegion;

    @Min(1) @Max(3)
    private Integer priority;

    @NotNull
    @Future
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime plannedDeparture;
}
