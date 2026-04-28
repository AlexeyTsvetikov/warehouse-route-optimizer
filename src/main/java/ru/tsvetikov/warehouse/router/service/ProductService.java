package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Category;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.db.repository.CategoryRepository;
import ru.tsvetikov.warehouse.router.model.db.repository.ProductRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.ProductRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.ProductResponse;
import ru.tsvetikov.warehouse.router.model.mapper.ProductMapper;
import ru.tsvetikov.warehouse.router.utils.PaginationUtils;


@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Cacheable(value = "products", key = "#sku")
    public Product getBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new CommonBackendException("Product not found: " + sku, HttpStatus.NOT_FOUND));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String formattedSku = formatSku(request.sku());
        checkSkuUniqueness(formattedSku);

        Category category = findCategoryByNameOrThrow(request.categoryName());

        Product product = productMapper.toEntity(request);
        product.setSku(formattedSku);
        product.setCategory(category);

        Product saved = productRepository.save(product);
        return productMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = findProductOrThrow(id);
        return productMapper.toResponseDto(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Integer page, Integer perPage,
                                        String sort, Sort.Direction order) {
        Pageable pageRequest = PaginationUtils.getPageRequest(page, perPage, sort, order);
        return productRepository.findAllByIsActiveTrue(pageRequest)
                .map(productMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getByCategory(Long categoryId, int page, int size, String sort, Sort.Direction order) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(order, sort));
        return productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable)
                .map(productMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String query, int page, int size, String sort, Sort.Direction order) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(order, sort));
        return productRepository.searchActive(query, pageable)
                .map(productMapper::toResponseDto);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProductOrThrow(id);

        if (request.sku() != null && !request.sku().equals(product.getSku())) {
            String formattedSku = formatSku(request.sku());
            checkSkuUniqueness(formattedSku);
            product.setSku(formattedSku);
        }


        if (request.categoryName() != null && !request.categoryName().equals(product.getCategory().getName())) {
            Category newCategory = findCategoryByNameOrThrow(request.categoryName());
            product.setCategory(newCategory);
        }

        productMapper.updateEntityFromDto(request, product);
        Product updated = productRepository.save(product);
        return productMapper.toResponseDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProductOrThrow(id);

        if (!product.getIsActive()) {
            throw new CommonBackendException("Product is already deleted", HttpStatus.CONFLICT);
        }

        product.setIsActive(false);
        productRepository.save(product);
    }

    @Transactional
    public ProductResponse activate(Long id) {
        Product product = findProductOrThrow(id);

        if (product.getIsActive()) {
            throw new CommonBackendException("Product is already active", HttpStatus.CONFLICT);
        }

        product.setIsActive(true);
        return productMapper.toResponseDto(product);
    }


    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Product with id: %s not found", id), HttpStatus.NOT_FOUND));
    }

    private Category findCategoryByNameOrThrow(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Category with name '%s' not found", categoryName), HttpStatus.NOT_FOUND));
    }

    private String formatSku(String sku) {
        if (sku == null || sku.isBlank()) return sku;
        return sku.toUpperCase();
    }

    private void checkSkuUniqueness(String sku) {
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new CommonBackendException(
                    String.format("Product with SKU '%s' already exists", sku), HttpStatus.CONFLICT);
        }
    }
}