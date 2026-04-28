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
import ru.tsvetikov.warehouse.router.model.dto.form.ProductForm;
import ru.tsvetikov.warehouse.router.model.dto.request.ProductRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.CategoryResponse;
import ru.tsvetikov.warehouse.router.model.dto.response.ProductResponse;
import ru.tsvetikov.warehouse.router.model.dto.response.StockResponse;
import ru.tsvetikov.warehouse.router.service.CategoryService;
import ru.tsvetikov.warehouse.router.service.ProductService;
import ru.tsvetikov.warehouse.router.service.StockService;

import java.util.List;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductWebController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final StockService stockService;

    @GetMapping
    public String list(
            @RequestParam(required = false) Long category,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "ASC") Sort.Direction order,
            Model model
    ) {
        Page<ProductResponse> products;
        if (category != null) {
            products = productService.getByCategory(category, page, size, sort, order);
        } else if (!search.isEmpty()) {
            products = productService.search(search, page, size, sort, order);
        } else {
            products = productService.getAll(page, size, sort, order);
        }

        Page<CategoryResponse> categories = categoryService.getAll(1, 100, "name", Sort.Direction.ASC);

        model.addAttribute("products", products);
        model.addAttribute("categories", categories.getContent());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        return "products/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("productForm", new ProductForm());
        Page<CategoryResponse> categories = categoryService.getAll(1, 100, "name", Sort.Direction.ASC);
        model.addAttribute("categories", categories.getContent());
        return "products/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("productForm") ProductForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Пожалуйста, исправьте ошибки в форме");
            Page<CategoryResponse> categories = categoryService.getAll(1, 100, "name", Sort.Direction.ASC);
            model.addAttribute("categories", categories.getContent());
            return "products/form";
        }

        try {
            ProductRequest request = new ProductRequest(
                    form.getSku(), form.getName(), form.getDescription(),
                    form.getWeight(), form.getWidth(), form.getHeight(), form.getDepth(),
                    form.getCategoryName()
            );
            productService.create(request);
            return "redirect:/products";
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("productForm", form);
            Page<CategoryResponse> categories = categoryService.getAll(1, 100, "name", Sort.Direction.ASC);
            model.addAttribute("categories", categories.getContent());
            return "products/form";
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        ProductResponse product = productService.getById(id);
        List<StockResponse> stocks = stockService.getByProduct(id);
        model.addAttribute("product", product);
        model.addAttribute("stocks", stocks);
        return "products/view";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        ProductResponse product = productService.getById(id);
        ProductForm form = new ProductForm();
        form.setSku(product.sku());
        form.setName(product.name());
        form.setDescription(product.description());
        form.setWeight(product.weight());
        form.setWidth(product.width());
        form.setHeight(product.height());
        form.setDepth(product.depth());
        form.setCategoryName(product.categoryName());

        model.addAttribute("productForm", form);
        model.addAttribute("id", id);
        Page<CategoryResponse> categories = categoryService.getAll(1, 100, "name", Sort.Direction.ASC);
        model.addAttribute("categories", categories.getContent());
        return "products/form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("productForm") ProductForm form,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Пожалуйста, исправьте ошибки в форме");
            model.addAttribute("id", id);
            Page<CategoryResponse> categories = categoryService.getAll(1, 100, "name", Sort.Direction.ASC);
            model.addAttribute("categories", categories.getContent());
            return "products/form";
        }

        try {
            ProductRequest request = new ProductRequest(
                    form.getSku(), form.getName(), form.getDescription(),
                    form.getWeight(), form.getWidth(), form.getHeight(), form.getDepth(),
                    form.getCategoryName()
            );
            productService.update(id, request);
            return "redirect:/products";
        } catch (CommonBackendException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("productForm", form);
            model.addAttribute("id", id);
            Page<CategoryResponse> categories = categoryService.getAll(1, 100, "name", Sort.Direction.ASC);
            model.addAttribute("categories", categories.getContent());
            return "products/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/products";
    }
}