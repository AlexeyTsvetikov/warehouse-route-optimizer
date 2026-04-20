package ru.tsvetikov.warehouse.router.controllers.web;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.tsvetikov.warehouse.router.model.dto.response.LocationResponse;
import ru.tsvetikov.warehouse.router.model.dto.response.StockResponse;
import ru.tsvetikov.warehouse.router.service.LocationService;
import ru.tsvetikov.warehouse.router.service.StockService;

import java.util.List;

@Controller
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockWebController {

    private final StockService stockService;
    private final LocationService locationService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Page<StockResponse> stocks = stockService.getFiltered(location, search, page, size);
        List<LocationResponse> locations = locationService.getAll(1, 100, "code", Sort.Direction.ASC).getContent();

        model.addAttribute("stocks", stocks);
        model.addAttribute("locations", locations);
        model.addAttribute("selectedLocation", location);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", stocks.getTotalPages());
        return "stocks/list";
    }
}
