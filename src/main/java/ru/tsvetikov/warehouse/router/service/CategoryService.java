package ru.tsvetikov.warehouse.router.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Category;
import ru.tsvetikov.warehouse.router.model.db.repository.CategoryRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.CategoryRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.CategoryResponse;
import ru.tsvetikov.warehouse.router.model.mapper.CategoryMapper;
import ru.tsvetikov.warehouse.router.utils.PaginationUtils;


@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String formattedName = formatName(request.name());
        checkNameUniqueness(formattedName);

        Category category = categoryMapper.toEntity(request);
        category.setName(formattedName);

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponseDto(savedCategory);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = findCategoryOrThrow(id);
        return categoryMapper.toResponseDto(category);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAll(Integer page, Integer perPage,
                                         String sort, Sort.Direction order) {
        Pageable pageRequest = PaginationUtils.getPageRequest(page, perPage, sort, order);

        return categoryRepository.findAllByIsActiveTrue(pageRequest)
                .map(categoryMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> search(String query, int page, int size, String sort, Sort.Direction order) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(order, sort));
        Page<Category> categories = categoryRepository.searchActive(query, pageable);
        return categories.map(categoryMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntityByCategoryName(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Category with name '%s' not found", categoryName), HttpStatus.NOT_FOUND));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findCategoryOrThrow(id);

        String formattedName = formatName(request.name());
        if (!category.getName().equalsIgnoreCase(formattedName)) {
            checkNameUniqueness(formattedName);
            category.setName(formattedName);
        }

        categoryMapper.updateEntityFromDto(request, category);

        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponseDto(updatedCategory);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findCategoryOrThrow(id);

        if (!category.getIsActive()) {
            throw new CommonBackendException("Category is already deleted", HttpStatus.CONFLICT);
        }

        category.setIsActive(false);
        categoryRepository.save(category);
    }

    @Transactional
    public CategoryResponse activate(Long id) {
        Category category = findCategoryOrThrow(id);

        if (category.getIsActive()) {
            throw new CommonBackendException("Category is already active", HttpStatus.CONFLICT);
        }

        category.setIsActive(true);
        return categoryMapper.toResponseDto(category);
    }


    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Category with id: %s not found", id), HttpStatus.NOT_FOUND));
    }

    private String formatName(String name) {
        if (name == null || name.isBlank()) return name;
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    private void checkNameUniqueness(String name) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new CommonBackendException("Category with name already exists", HttpStatus.CONFLICT);
        }
    }
}
