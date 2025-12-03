package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.db.repository.ProductRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.ProductRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.ProductResponse;
import ru.tsvetikov.warehouse.router.model.mapper.ProductMapper;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        log.info("Creating product with tracking number: {}", request.trackingNumber());

        if (productRepository.findByTrackingNumber(request.trackingNumber()).isPresent()) {
            throw new CommonBackendException(
                    "Product with tracking number already exists: " + request.trackingNumber(), HttpStatus.CONFLICT);
        }

        Product product = productMapper.toEntity(request);

        Product saved = productRepository.save(product);
        log.info("Product created with id: {}", saved.getId());

        return productMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        log.info("Fetching product with id: {}", id);

        Product product = productRepository.findById(id).
                orElseThrow(() -> new CommonBackendException("Product not found with id: " + id, HttpStatus.NOT_FOUND));

        return productMapper.toResponseDto(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream()
                .map(productMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting product with id: {}", id);

        if (!productRepository.existsById(id)) {
            throw new CommonBackendException("Product not found with id: " + id, HttpStatus.NOT_FOUND);
        }

        productRepository.deleteById(id);
        log.info("Product deleted with id: {}", id);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);

        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new CommonBackendException("Product not found with id: " + id,
                        HttpStatus.NOT_FOUND));

        if (request.trackingNumber() != null && !request.trackingNumber().equals(existing.getTrackingNumber())) {
            if (productRepository.findByTrackingNumber(request.trackingNumber()).isPresent()) {
                throw new CommonBackendException("Product with tracking number already exists: "
                        + request.trackingNumber(), HttpStatus.CONFLICT);
            }
        }

        updateProductFields(existing, request);

        Product updated = productRepository.save(existing);
        log.info("Product updated with id: {}", id);

        return productMapper.toResponseDto(updated);
    }

    private void updateProductFields(Product product, ProductRequest request) {
        if (request.trackingNumber() != null) product.setTrackingNumber(request.trackingNumber());
        if (request.destinationRegion() != null) product.setDestinationRegion(request.destinationRegion());
        if (request.width() != null) product.setWidth(request.width());
        if (request.height() != null) product.setHeight(request.height());
        if (request.depth() != null) product.setDepth(request.depth());
        if (request.weight() != null) product.setWeight(request.weight());
        if (request.priority() != null) product.setPriority(request.priority());
    }
}
