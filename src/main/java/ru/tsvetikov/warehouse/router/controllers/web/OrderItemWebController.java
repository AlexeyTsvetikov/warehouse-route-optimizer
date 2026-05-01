package ru.tsvetikov.warehouse.router.controllers.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.dto.form.OrderItemForm;
import ru.tsvetikov.warehouse.router.model.dto.request.OrderItemRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.OrderItemResponse;
import ru.tsvetikov.warehouse.router.service.OrderItemService;
import ru.tsvetikov.warehouse.router.service.ProductService;
import ru.tsvetikov.warehouse.router.utils.ValidationUtils;

@Controller
@RequestMapping("/orders/{orderNumber}/items")
@RequiredArgsConstructor
public class OrderItemWebController {

    private final OrderItemService orderItemService;
    private final ProductService productService;

    @GetMapping("/create")
    public String createForm(@PathVariable String orderNumber, Model model) {
        model.addAttribute("orderNumber", orderNumber);
        model.addAttribute("orderItemForm", new OrderItemForm());
        model.addAttribute("products", productService.getAll(1, 1000, "name", Sort.Direction.ASC).getContent());
        return "orders/items/form";
    }

    @PostMapping("/create")
    public String create(@PathVariable String orderNumber,
                         @Valid @ModelAttribute("orderItemForm") OrderItemForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("orderNumber", orderNumber);
            model.addAttribute("products", productService.getAll(1, 1000, "name", Sort.Direction.ASC).getContent());
            model.addAttribute("errorMessage", "Ошибки в форме: " + ValidationUtils.getValidationErrors(result));
            return "orders/items/form";
        }

        try {
            OrderItemRequest request = new OrderItemRequest(form.getProductSku(), form.getQuantity());
            orderItemService.create(orderNumber, request);
            return String.format("redirect:/orders/%s#items", orderNumber);
        } catch (CommonBackendException e) {
            model.addAttribute("orderNumber", orderNumber);
            model.addAttribute("orderItemForm", form);
            model.addAttribute("products", productService.getAll(1, 1000, "name", Sort.Direction.ASC).getContent());
            model.addAttribute("errorMessage", e.getMessage());
            return "orders/items/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable String orderNumber, @PathVariable Long id, Model model) {
        OrderItemResponse item = orderItemService.getById(orderNumber, id);
        OrderItemForm form = new OrderItemForm();
        form.setProductSku(item.productSku());
        form.setQuantity(item.quantity());
        model.addAttribute("orderNumber", orderNumber);
        model.addAttribute("id", id);
        model.addAttribute("orderItemForm", form);
        model.addAttribute("selectedProductText", item.productSku() + " - " + item.productName());
        model.addAttribute("products", productService.getAll(1, 1000, "name", Sort.Direction.ASC).getContent());
        return "orders/items/form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable String orderNumber,
                         @PathVariable Long id,
                         @Valid @ModelAttribute("orderItemForm") OrderItemForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("orderNumber", orderNumber);
            model.addAttribute("id", id);
            model.addAttribute("products", productService.getAll(1, 1000, "name", Sort.Direction.ASC).getContent());
            model.addAttribute("errorMessage", "Ошибки в форме: " + ValidationUtils.getValidationErrors(result));
            return "orders/items/form";
        }

        try {
            OrderItemRequest request = new OrderItemRequest(form.getProductSku(), form.getQuantity());
            orderItemService.update(orderNumber, id, request);
            return String.format("redirect:/orders/%s#items", orderNumber);
        } catch (CommonBackendException e) {
            model.addAttribute("orderNumber", orderNumber);
            model.addAttribute("id", id);
            model.addAttribute("orderItemForm", form);
            model.addAttribute("products", productService.getAll(1, 1000, "name", Sort.Direction.ASC).getContent());
            model.addAttribute("errorMessage", e.getMessage());
            return "orders/items/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable String orderNumber, @PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderItemService.delete(orderNumber, id);
        } catch (CommonBackendException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return String.format("redirect:/orders/%s#items", orderNumber);
    }
}