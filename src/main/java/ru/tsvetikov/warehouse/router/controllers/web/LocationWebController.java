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
import ru.tsvetikov.warehouse.router.model.dto.form.LocationForm;
import ru.tsvetikov.warehouse.router.model.dto.request.LocationRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.LocationResponse;
import ru.tsvetikov.warehouse.router.service.LocationService;

@Controller
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationWebController {

    private final LocationService locationService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "code") String sort,
            @RequestParam(defaultValue = "ASC") Sort.Direction order,
            Model model
    ) {
        Page<LocationResponse> locations = locationService.search(search, page, size, sort, order);
        model.addAttribute("locations", locations);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", locations.getTotalPages());
        return "locations/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("locationForm", new LocationForm());
        return "locations/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("locationForm") LocationForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Пожалуйста, исправьте ошибки в форме");
            return "locations/form";
        }

        try {
            LocationRequest request = new LocationRequest(
                    form.getCode(),
                    form.getType(),
                    form.getWidth(),
                    form.getHeight(),
                    form.getDepth(),
                    form.getMaxWeight(),
                    form.getCoordX(),
                    form.getCoordY(),
                    form.getDescription()
            );
            locationService.create(request);
            return "redirect:/locations";
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("locationForm", form);
            return "locations/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        LocationResponse location = locationService.getById(id);
        LocationForm form = new LocationForm();
        form.setCode(location.code());
        form.setType(location.type());
        form.setWidth(location.width());
        form.setHeight(location.height());
        form.setDepth(location.depth());
        form.setMaxWeight(location.maxWeight());
        form.setCoordX(location.coordX());
        form.setCoordY(location.coordY());
        form.setDescription(location.description());
        form.setIsActive(location.isActive());
        model.addAttribute("locationForm", form);
        model.addAttribute("id", id);
        return "locations/form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("locationForm") LocationForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Пожалуйста, исправьте ошибки в форме");
            model.addAttribute("id", id);
            return "locations/form";
        }

        try {
            locationService.updateFromWeb(id, form);
            return "redirect:/locations";
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("locationForm", form);
            model.addAttribute("id", id);
            return "locations/form";
        }
    }

    @PostMapping("/deactivate/{id}")
    public String deactivate(@PathVariable Long id) {
        locationService.delete(id);
        return "redirect:/locations";
    }
}