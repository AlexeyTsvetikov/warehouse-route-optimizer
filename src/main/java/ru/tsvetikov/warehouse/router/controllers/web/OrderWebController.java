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
import ru.tsvetikov.warehouse.router.model.dto.form.OrderForm;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderItemResponse;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderResponse;
import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;
import ru.tsvetikov.warehouse.router.model.enums.OrderType;
import ru.tsvetikov.warehouse.router.service.OrderItemService;
import ru.tsvetikov.warehouse.router.service.OrderService;
import ru.tsvetikov.warehouse.router.utils.ValidationUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderWebController {

    private final OrderService orderService;
    private final OrderItemService orderItemService;

    @GetMapping
    public String list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderNumber") String sort,
            @RequestParam(defaultValue = "ASC") Sort.Direction order,
            Model model
    ) {
        Page<OrderResponse> orders;

        if (!search.isEmpty()) {
            orders = orderService.search(search, page, size, sort, order);
        } else if (status != null) {
            orders = orderService.getByStatuses(List.of(status), page, size, sort, order);
        } else {
            orders = orderService.getAll(page, size, sort, order);
        }

        model.addAttribute("orders", orders);
        model.addAttribute("search", search);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());
        model.addAttribute("allStatuses", OrderStatus.values());

        return "orders/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("orderForm", new OrderForm());
        model.addAttribute("orderTypes", OrderType.values());
        return "orders/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("orderForm") OrderForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Ошибки в форме: " + ValidationUtils.getValidationErrors(result));
            model.addAttribute("orderTypes", OrderType.values());
            return "orders/form";
        }

        try {
            Instant plannedDepartureInstant = form.getPlannedDeparture()
                    .atZone(ZoneId.systemDefault())
                    .toInstant();

            OrderRequest request = new OrderRequest(
                    form.getType(),
                    form.getCustomerName(),
                    form.getDestinationRegion(),
                    form.getPriority(),
                    plannedDepartureInstant
            );
            orderService.createEmptyOrder(request);
            return "redirect:/orders";
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("orderForm", form);
            model.addAttribute("orderTypes", OrderType.values());
            return "orders/form";
        }
    }

    @GetMapping("/{orderNumber}")
    public String view(@PathVariable String orderNumber, Model model) {
        OrderResponse order = orderService.getByNumber(orderNumber);
        List<OrderItemResponse> items = orderItemService.getByOrderForWeb(orderNumber);
        model.addAttribute("order", order);
        model.addAttribute("items", items);
        return "orders/view";
    }

    @GetMapping("/edit/{orderNumber}")
    public String editForm(@PathVariable String orderNumber, Model model) {
        OrderResponse order = orderService.getByNumber(orderNumber);
        OrderForm form = new OrderForm();
        form.setType(order.type());
        form.setCustomerName(order.customerName());
        form.setDestinationRegion(order.destinationRegion());
        form.setPriority(order.priority());
        form.setPlannedDeparture(order.plannedDeparture()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime());

        model.addAttribute("orderForm", form);
        model.addAttribute("orderNumber", orderNumber);
        model.addAttribute("orderTypes", OrderType.values());
        return "orders/form";
    }

    @PostMapping("/edit/{orderNumber}")
    public String update(@PathVariable String orderNumber,
                         @Valid @ModelAttribute("orderForm") OrderForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Ошибки в форме: " + ValidationUtils.getValidationErrors(result));
            model.addAttribute("orderNumber", orderNumber);
            model.addAttribute("orderTypes", OrderType.values());
            return "orders/form";
        }

        try {
            Instant plannedDepartureInstant = form.getPlannedDeparture()
                    .atZone(ZoneId.systemDefault())
                    .toInstant();

            OrderRequest request = new OrderRequest(
                    form.getType(),
                    form.getCustomerName(),
                    form.getDestinationRegion(),
                    form.getPriority(),
                    plannedDepartureInstant
            );
            orderService.update(orderNumber, request);
            return "redirect:/orders";
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("orderForm", form);
            model.addAttribute("orderNumber", orderNumber);
            model.addAttribute("orderTypes", OrderType.values());
            return "orders/form";
        }
    }

    @PostMapping("/start/{orderNumber}")
    public String startProcessing(@PathVariable String orderNumber, RedirectAttributes redirectAttributes) {
        try {
            orderService.startProcessing(orderNumber);
        } catch (CommonBackendException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/orders/" + orderNumber;
    }

    @PostMapping("/complete/{orderNumber}")
    public String complete(@PathVariable String orderNumber, RedirectAttributes redirectAttributes) {
        try {
            orderService.completeOrder(orderNumber);
        } catch (CommonBackendException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/orders/" + orderNumber;
    }

    @PostMapping("/cancel/{orderNumber}")
    public String cancel(@PathVariable String orderNumber, Model model) {
        try {
            orderService.cancelOrder(orderNumber);
        } catch (CommonBackendException e) {
            OrderResponse order = orderService.getByNumber(orderNumber);
            List<OrderItemResponse> items = orderItemService.getByOrderForWeb(orderNumber);
            model.addAttribute("order", order);
            model.addAttribute("items", items);
            model.addAttribute("errorMessage", e.getMessage());
            return "orders/view";
        }
        return "redirect:/orders";
    }
}