package ru.tsvetikov.warehouse.router.controllers.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.dto.form.WarehouseTaskForm;
import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.WarehouseTaskResponse;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskStatus;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;
import ru.tsvetikov.warehouse.router.service.UserService;
import ru.tsvetikov.warehouse.router.service.WarehouseTaskService;
import ru.tsvetikov.warehouse.router.utils.ValidationUtils;

@Controller
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class WarehouseTaskWebController {

    private final WarehouseTaskService warehouseTaskService;
    private final UserService userService;

    @GetMapping
    public String list(
            @RequestParam(required = false) WarehouseTaskStatus status,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction order,
            Model model
    ) {
        Page<WarehouseTaskResponse> tasks;

        if (!search.isEmpty()) {
            tasks = warehouseTaskService.search(search, page, size, sort, order);
        } else if (status != null) {
            tasks = warehouseTaskService.getByStatus(status, page, size, sort, order);
        } else {
            tasks = warehouseTaskService.getAll(page, size, sort, order);
        }

        model.addAttribute("tasks", tasks);
        model.addAttribute("search", search);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", tasks.getTotalPages());
        model.addAttribute("allStatuses", WarehouseTaskStatus.values());

        return "tasks/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("taskForm", new WarehouseTaskForm());
        model.addAttribute("taskTypes", WarehouseTaskType.values());
        model.addAttribute("users", userService.getAllActiveUsers());
        return "tasks/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("taskForm") WarehouseTaskForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Ошибки в форме: " + ValidationUtils.getValidationErrors(result));
            model.addAttribute("taskTypes", WarehouseTaskType.values());
            model.addAttribute("users", userService.getAllActiveUsers());
            return "tasks/form";
        }

        try {
            WarehouseTaskRequest request = WarehouseTaskRequest.builder()
                    .type(form.getType())
                    .productSku(form.getProductSku())
                    .plannedQuantity(form.getPlannedQuantity())
                    .sourceLocationCode(form.getSourceLocationCode())
                    .targetLocationCode(form.getTargetLocationCode())
                    .assignedUsername(form.getAssignedUsername())
                    .orderNumber(form.getOrderNumber())
                    .confirmedQuantity(form.getConfirmedQuantity())
                    .build();

            warehouseTaskService.create(request);
            return "redirect:/tasks";
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("taskForm", form);
            model.addAttribute("taskTypes", WarehouseTaskType.values());
            model.addAttribute("users", userService.getAllActiveUsers());
            model.addAttribute("selectedProductText", form.getProductSku());
            model.addAttribute("selectedSourceLocationText", form.getSourceLocationCode());
            model.addAttribute("selectedTargetLocationText", form.getTargetLocationCode());
            model.addAttribute("selectedOperatorText", form.getAssignedUsername());
            model.addAttribute("selectedOrderText", form.getOrderNumber());
            return "tasks/form";
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        WarehouseTaskResponse task = warehouseTaskService.getById(id);
        model.addAttribute("task", task);
        return "tasks/view";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        WarehouseTaskResponse task = warehouseTaskService.getById(id);
        WarehouseTaskForm form = new WarehouseTaskForm();
        form.setType(task.type());
        form.setProductSku(task.productSku());
        form.setPlannedQuantity(task.plannedQuantity());
        form.setSourceLocationCode(task.sourceLocationCode());
        form.setTargetLocationCode(task.targetLocationCode());
        form.setAssignedUsername(task.assignedUsername());
        form.setOrderNumber(task.orderNumber());

        model.addAttribute("taskForm", form);
        model.addAttribute("id", id);
        model.addAttribute("taskTypes", WarehouseTaskType.values());
        model.addAttribute("users", userService.getAllActiveUsers());

        if (task.productSku() != null) {
            model.addAttribute("selectedProductText", task.productSku());
        }
        if (task.sourceLocationCode() != null) {
            model.addAttribute("selectedSourceLocationText", task.sourceLocationCode());
        }
        if (task.targetLocationCode() != null) {
            model.addAttribute("selectedTargetLocationText", task.targetLocationCode());
        }
        if (task.assignedUsername() != null) {
            model.addAttribute("selectedOperatorText", task.assignedUsername());
        }
        if (task.orderNumber() != null) {
            model.addAttribute("selectedOrderText", task.orderNumber());
        }

        return "tasks/form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("taskForm") WarehouseTaskForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Ошибки в форме: " + ValidationUtils.getValidationErrors(result));
            model.addAttribute("id", id);
            model.addAttribute("taskTypes", WarehouseTaskType.values());
            model.addAttribute("users", userService.getAllActiveUsers());
            return "tasks/form";
        }

        try {
            WarehouseTaskRequest request = WarehouseTaskRequest.builder()
                    .type(form.getType())
                    .productSku(form.getProductSku())
                    .plannedQuantity(form.getPlannedQuantity())
                    .sourceLocationCode(form.getSourceLocationCode())
                    .targetLocationCode(form.getTargetLocationCode())
                    .assignedUsername(form.getAssignedUsername())
                    .orderNumber(form.getOrderNumber())
                    .confirmedQuantity(form.getConfirmedQuantity())
                    .build();

            warehouseTaskService.update(id, request);
            return "redirect:/tasks";
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("taskForm", form);
            model.addAttribute("id", id);
            model.addAttribute("taskTypes", WarehouseTaskType.values());
            model.addAttribute("users", userService.getAllActiveUsers());
            model.addAttribute("selectedProductText", form.getProductSku());
            model.addAttribute("selectedSourceLocationText", form.getSourceLocationCode());
            model.addAttribute("selectedTargetLocationText", form.getTargetLocationCode());
            model.addAttribute("selectedOperatorText", form.getAssignedUsername());
            model.addAttribute("selectedOrderText", form.getOrderNumber());
            return "tasks/form";
        }
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam String username, Model model) {
        if (username == null || username.isBlank()) {
            WarehouseTaskResponse task = warehouseTaskService.getById(id);
            model.addAttribute("task", task);
            model.addAttribute("errorMessage", "Не выбран оператор. Выберите оператора из списка.");
            return "tasks/view";
        }
        try {
            warehouseTaskService.assignTask(id, username);
        } catch (CommonBackendException e) {
            WarehouseTaskResponse task = warehouseTaskService.getById(id);
            model.addAttribute("task", task);
            model.addAttribute("errorMessage", e.getMessage());
            return "tasks/view";
        }
        return "redirect:/tasks/" + id;
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable Long id, Model model) {
        try {
            warehouseTaskService.startTask(id);
        } catch (CommonBackendException e) {
            WarehouseTaskResponse task = warehouseTaskService.getById(id);
            model.addAttribute("task", task);
            model.addAttribute("errorMessage", e.getMessage());
            return "tasks/view";
        }
        return "redirect:/tasks/" + id;
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id,
                           @RequestParam(required = false) Integer confirmedQuantity,
                           Model model) {
        try {
            warehouseTaskService.completeTask(id, confirmedQuantity);
            return "redirect:/tasks/" + id;
        } catch (CommonBackendException e) {
            WarehouseTaskResponse task = warehouseTaskService.getById(id);
            model.addAttribute("task", task);
            model.addAttribute("errorMessage", e.getMessage());
            return "tasks/view";
        }
    }
}