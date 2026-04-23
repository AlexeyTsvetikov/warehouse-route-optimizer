package ru.tsvetikov.warehouse.router.model.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.tsvetikov.warehouse.router.model.enums.Role;

@Getter
@Setter
@NoArgsConstructor
public class UserEditForm {
    @NotBlank(message = "Имя обязательно")
    @Size(min = 3, max = 50)
    private String username;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @NotNull(message = "Роль обязательна")
    private Role role;
}
