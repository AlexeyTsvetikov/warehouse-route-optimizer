package ru.tsvetikov.warehouse.router.controllers.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.dto.form.UserEditForm;
import ru.tsvetikov.warehouse.router.model.dto.form.UserCreateForm;
import ru.tsvetikov.warehouse.router.model.dto.request.ChangePasswordRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.UserCreateRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.UserUpdateRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.UserResponse;
import ru.tsvetikov.warehouse.router.model.enums.Role;
import ru.tsvetikov.warehouse.router.service.UserService;
import ru.tsvetikov.warehouse.router.utils.ValidationUtils;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserWebController {

    private final UserService userService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sort,
            @RequestParam(defaultValue = "ASC") Sort.Direction order,
            Model model
    ) {
        Page<UserResponse> users;
        if (role != null && !role.isBlank()) {
            users = userService.getByRole(Role.valueOf(role), page, size, sort, order);
        } else if (!search.isEmpty()) {
            users = userService.search(search, page, size, sort, order);
        } else {
            users = userService.getAll(page, size, sort, order);
        }

        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("selectedRole", role);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());

        return "users/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("userForm", new UserCreateForm());
        return "users/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("userForm") UserCreateForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Ошибки в форме: " + ValidationUtils.getValidationErrors(result));
            return "users/form";
        }

        try {
            UserCreateRequest request = new UserCreateRequest(
                    form.getUsername(),
                    form.getPassword(),
                    form.getFirstName(),
                    form.getLastName(),
                    form.getRole());
            userService.create(request);
            return "redirect:/users";
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("userForm", form);
            return "users/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        UserResponse user = userService.getById(id);
        UserEditForm form = new UserEditForm();
        form.setUsername(user.username());
        form.setFirstName(user.firstName());
        form.setLastName(user.lastName());
        form.setRole(user.role());
        model.addAttribute("userForm", form);
        model.addAttribute("id", id);
        return "users/form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("userForm") UserEditForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Ошибки в форме: " + ValidationUtils.getValidationErrors(result));
            model.addAttribute("id", id);
            return "users/form";
        }

        try {
            UserUpdateRequest request = new UserUpdateRequest(
                    form.getUsername(),
                    form.getFirstName(),
                    form.getLastName(),
                    form.getRole());
            userService.update(id, request);
            return "redirect:/users";
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("userForm", form);
            model.addAttribute("id", id);
            return "users/form";
        }
    }

    @GetMapping("/change-password")
    public String showChangePasswordFormForSelf() {
        return "users/change-password";
    }


    @PostMapping("/change-password")
    public String changePasswordForSelf(@Valid @ModelAttribute ChangePasswordRequest request,
                                        BindingResult result,
                                        Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Ошибки в форме: " + ValidationUtils.getValidationErrors(result));
            return "users/change-password";
        }
        try {
            userService.changePasswordForCurrentUser(request);
            return "redirect:/dashboard";
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "users/change-password";
        }
    }


    @GetMapping("/change-password/{id}")
    public String showChangePasswordFormForUser(@PathVariable Long id, Model model) {
        model.addAttribute("userId", id);
        return "users/change-password";
    }


    @PostMapping("/change-password/{id}")
    public String changePasswordForUser(@PathVariable Long id,
                                        @Valid @ModelAttribute ChangePasswordRequest request,
                                        BindingResult result,
                                        Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Ошибки в форме: " + ValidationUtils.getValidationErrors(result));
            model.addAttribute("userId", id);
            return "users/change-password";
        }
        try {
            userService.changePassword(id, request);
            return "redirect:/users/edit/" + id;
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("userId", id);
            return "users/change-password";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.delete(id);
        } catch (CommonBackendException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/users";
    }
}