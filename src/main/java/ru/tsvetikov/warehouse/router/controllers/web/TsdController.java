package ru.tsvetikov.warehouse.router.controllers.web;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.tsvetikov.warehouse.router.model.dto.response.*;
import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskStatus;
import ru.tsvetikov.warehouse.router.service.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/tsd")
public class TsdController {

    private final WarehouseTaskService taskService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final StockService stockService;
    private final ProductService productService;
    private final OrderService orderService;
    private final RoutingService routingService;

    @GetMapping
    public String index() {
        return "redirect:/tsd/tasks";
    }

    @GetMapping("/tasks")
    public String taskList(@RequestParam(required = false) WarehouseTaskStatus status,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        if (status == null) {
            status = WarehouseTaskStatus.CREATED;
        }
        Page<WarehouseTaskResponse> tasks = taskService.getByStatus(status, page, 20, "createdAt", Sort.Direction.DESC);
        model.addAttribute("tasks", tasks.getContent());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", tasks.getTotalPages());
        model.addAttribute("content", "tsd/task-list");
        return "tsd/layout-tsd";
    }

    @GetMapping("/tasks/{id}")
    public String taskView(@PathVariable Long id, Model model) {
        WarehouseTaskResponse task = taskService.getById(id);
        model.addAttribute("task", task);
        model.addAttribute("tsd", true);
        return "tasks/view";
    }

    @GetMapping("/categories")
    public String categoryList(@RequestParam(defaultValue = "") String search,
                               @RequestParam(defaultValue = "1") int page,
                               Model model) {
        Page<CategoryResponse> categories = categoryService.search(search, page, 15, "name", Sort.Direction.ASC);
        model.addAttribute("categories", categories.getContent());
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categories.getTotalPages());
        model.addAttribute("content", "tsd/category-list");
        return "tsd/layout-tsd";
    }

    @GetMapping("/locations")
    public String locationList(@RequestParam(defaultValue = "") String search,
                               @RequestParam(defaultValue = "1") int page,
                               Model model) {
        Page<LocationResponse> locations = locationService.search(search, page, 15, "code", Sort.Direction.ASC);
        model.addAttribute("locations", locations.getContent());
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", locations.getTotalPages());
        model.addAttribute("content", "tsd/location-list");
        return "tsd/layout-tsd";
    }

    @GetMapping("/stocks")
    public String stockList(@RequestParam(required = false) String location,
                            @RequestParam(defaultValue = "") String search,
                            @RequestParam(defaultValue = "1") int page,
                            Model model) {
        Page<StockResponse> stocks = stockService.getFiltered(location, search, page, 20);
        List<LocationResponse> locations = locationService.getAll(1, 100, "code", Sort.Direction.ASC).getContent();

        model.addAttribute("stocks", stocks.getContent());
        model.addAttribute("locations", locations);
        model.addAttribute("selectedLocation", location);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", stocks.getTotalPages());
        model.addAttribute("content", "tsd/stock-list");
        return "tsd/layout-tsd";
    }

    @GetMapping("/products")
    public String productList(@RequestParam(required = false) Long category,
                              @RequestParam(defaultValue = "") String search,
                              @RequestParam(defaultValue = "1") int page,
                              Model model) {
        Page<ProductResponse> products;
        if (category != null) {
            products = productService.getByCategory(category, page, 20, "name", Sort.Direction.ASC);
        } else if (!search.isEmpty()) {
            products = productService.search(search, page, 20, "name", Sort.Direction.ASC);
        } else {
            products = productService.getAll(page, 20, "name", Sort.Direction.ASC);
        }

        Page<CategoryResponse> categories = categoryService.getAll(1, 100, "name", Sort.Direction.ASC);

        model.addAttribute("products", products.getContent());
        model.addAttribute("categories", categories.getContent());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("content", "tsd/product-list");
        return "tsd/layout-tsd";
    }

    @GetMapping("/orders")
    public String orderList(@RequestParam(required = false) OrderStatus status,
                            @RequestParam(defaultValue = "") String search,
                            @RequestParam(defaultValue = "1") int page,
                            Model model) {
        Page<OrderResponse> orders;
        if (!search.isEmpty()) {
            orders = orderService.search(search, page, 20, "orderNumber", Sort.Direction.ASC);
        } else if (status != null) {
            orders = orderService.getByStatuses(List.of(status), page, 20, "orderNumber", Sort.Direction.ASC);
        } else {
            orders = orderService.getAll(page, 20, "orderNumber", Sort.Direction.ASC);
        }

        model.addAttribute("orders", orders.getContent());
        model.addAttribute("allStatuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());
        model.addAttribute("content", "tsd/order-list");
        return "tsd/layout-tsd";
    }

    @GetMapping("/tasks/route")
    public String showRoute(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        List<WarehouseTaskResponse> tasks = taskService
                .getTasksByUserAndStatus(username,
                        List.of(WarehouseTaskStatus.ASSIGNED, WarehouseTaskStatus.IN_PROGRESS),
                        1, 100, "createdAt", Sort.Direction.ASC)
                .getContent();

        RouteResponse result = routingService.calculateCombinedRoute(tasks, 0.0, 0.0);

        model.addAttribute("result", result);
        model.addAttribute("content", "/tsd/route");
        return "tsd/layout-tsd";
    }

}