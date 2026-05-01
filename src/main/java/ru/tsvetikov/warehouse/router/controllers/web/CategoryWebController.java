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
import ru.tsvetikov.warehouse.router.model.dto.form.CategoryForm;
import ru.tsvetikov.warehouse.router.model.dto.request.CategoryRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.CategoryResponse;
import ru.tsvetikov.warehouse.router.service.CategoryService;
import ru.tsvetikov.warehouse.router.utils.ValidationUtils;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryWebController {

    private final CategoryService categoryService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "ASC") Sort.Direction order,
            Model model
    ) {
        Page<CategoryResponse> categories = categoryService.search(search, page, size, sort, order);
        model.addAttribute("categories", categories);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categories.getTotalPages());
        return "categories/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        return "categories/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Ошибки в форме: " + ValidationUtils.getValidationErrors(result));
            return "categories/form";
        }

        try {
            CategoryRequest request = new CategoryRequest(form.getName(), form.getDescription());
            categoryService.create(request);
            return "redirect:/categories";
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("categoryForm", form);
            return "categories/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        CategoryResponse category = categoryService.getById(id);
        CategoryForm form = new CategoryForm();
        form.setName(category.name());
        form.setDescription(category.description());
        model.addAttribute("categoryForm", form);
        model.addAttribute("id", id);
        return "categories/form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Ошибки в форме: " + ValidationUtils.getValidationErrors(result));
            model.addAttribute("id", id);
            return "categories/form";
        }

        try {
            CategoryRequest request = new CategoryRequest(form.getName(), form.getDescription());
            categoryService.update(id, request);
            return "redirect:/categories";
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("categoryForm", form);
            model.addAttribute("id", id);
            return "categories/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
        } catch (CommonBackendException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/categories";
    }
}