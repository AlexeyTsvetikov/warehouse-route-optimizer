package ru.tsvetikov.warehouse.router.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Category;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.db.repository.ProductRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.ProductRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.ProductResponse;
import ru.tsvetikov.warehouse.router.model.mapper.ProductMapper;
import ru.tsvetikov.warehouse.router.testdata.ProductTestData;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Mock
    private CategoryService categoryService;

    @Test
    void shouldReturnProductBySku() {
        String sku = "BRP-ENG-001";
        Product entity = ProductTestData.entityBuilder().build();

        when(productRepository.findBySkuIgnoreCase(sku)).thenReturn(Optional.of(entity));

        Product result = productService.getBySku(sku);

        assertThat(result).isEqualTo(entity);
        verify(productRepository).findBySkuIgnoreCase(sku);
    }

    @Test
    void shouldThrowWhenProductNotFoundBySku() {
        String sku = "BRP-ENG-002";

        when(productRepository.findBySkuIgnoreCase(sku)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getBySku(sku))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Product with SKU '%s' not found", sku);

        verify(productRepository).findBySkuIgnoreCase("BRP-ENG-002");
    }

    @Test
    void shouldThrowWhenProductSkuIsNull() {
        assertThatThrownBy(() -> productService.getBySku(null))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Sku must not be blank");

        verifyNoInteractions(productRepository);
    }

    @Test
    void shouldThrowWhenProductSkuIsBlank() {
        assertThatThrownBy(() -> productService.getBySku("   "))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Sku must not be blank");

        verifyNoInteractions(productRepository);
    }

    @Test
    void shouldCreateProduct() {
        ProductRequest request = ProductTestData.request();
        Product product = ProductTestData.entityBuilder().build();
        Category category = Category.builder().name("Двигатель и топливная система").build();
        ProductResponse response = ProductTestData.response();

        when(productRepository.existsBySkuIgnoreCase("BRP-ENG-001")).thenReturn(false);
        when(categoryService.getCategoryEntityByCategoryName(request.categoryName())).thenReturn(category);
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponseDto(product)).thenReturn(response);

        ProductResponse result = productService.create(request);

        assertThat(result).isEqualTo(response);
        assertThat(product.getSku()).isEqualTo("BRP-ENG-001");
        assertThat(product.getCategory()).isEqualTo(category);
        verify(productRepository).existsBySkuIgnoreCase("BRP-ENG-001");
        verify(productRepository).save(product);
    }

    @Test
    void shouldThrowWhenSkuIsBlankOnCreate() {
        ProductRequest request = ProductTestData.request("   ");

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Sku must not be blank");

        verifyNoInteractions(productMapper);
        verifyNoInteractions(categoryService);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSkuIsNullOnCreate() {
        ProductRequest request = ProductTestData.request(null);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Sku must not be blank");

        verifyNoInteractions(productMapper);
        verifyNoInteractions(categoryService);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSkuAlreadyExistOnCreate() {
        ProductRequest request = ProductTestData.request();

        when(productRepository.existsBySkuIgnoreCase("BRP-ENG-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Product with SKU '%s' already exists", "BRP-ENG-001");

        verify(productRepository).existsBySkuIgnoreCase("BRP-ENG-001");
        verifyNoInteractions(categoryService);
        verifyNoInteractions(productMapper);
        verify(productRepository, never()).save(any());
    }


    @Test
    void shouldReturnProductById() {
        Long id = 1L;
        Product entity = ProductTestData.entityBuilder().id(1L).build();
        ProductResponse response = ProductTestData.response();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(productMapper.toResponseDto(entity)).thenReturn(response);

        ProductResponse result = productService.getById(id);

        assertThat(result).isEqualTo(response);
        verify(productRepository).findById(id);
        verify(productMapper).toResponseDto(entity);
    }

    @Test
    void shouldThrowWhenProductNotFoundOnGetById() {
        Long id = 99L;

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Product with id: %s not found", id);

        verify(productRepository).findById(id);
        verifyNoInteractions(productMapper);
    }

    @Test
    void shouldSearchProducts() {
        String query = "brp";
        Pageable pageable = PageRequest.of(0, 10);
        Product entity = ProductTestData.entityBuilder().id(1L).build();
        ProductResponse response = ProductTestData.response();
        Page<Product> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(productRepository.searchActive(eq(query), any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponseDto(entity)).thenReturn(response);

        Page<ProductResponse> result = productService.search(query, 0, 10, "name", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(productRepository).searchActive(eq(query), any(Pageable.class));
        verify(productMapper).toResponseDto(entity);
    }

    @Test
    void shouldReturnPageOfProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Product entity = ProductTestData.entityBuilder().id(1L).build();
        ProductResponse response = ProductTestData.response();
        Page<Product> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(productRepository.findAllByIsActiveTrue(any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponseDto(entity)).thenReturn(response);

        Page<ProductResponse> result = productService.getAll(0, 10, "name", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(productRepository).findAllByIsActiveTrue(any(Pageable.class));
        verify(productMapper).toResponseDto(entity);
    }

    @Test
    void shouldReturnPageOfProductsByCategoryId() {
        Long categoryId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Product entity = ProductTestData.entityBuilder().id(1L).build();
        ProductResponse response = ProductTestData.response();
        Page<Product> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(productRepository.findByCategoryIdAndIsActiveTrue(eq(categoryId), any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponseDto(entity)).thenReturn(response);

        Page<ProductResponse> result = productService.getByCategory(categoryId, 0, 10, "name", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(productRepository).findByCategoryIdAndIsActiveTrue(eq(categoryId), any(Pageable.class));
        verify(productMapper).toResponseDto(entity);
    }

    @Test
    void shouldUpdateProduct() {
        Long id = 1L;
        ProductRequest request = ProductTestData.request();
        Product entity = ProductTestData.entityBuilder().id(1L).build();
        ProductResponse response = ProductTestData.response();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponseDto(entity)).thenReturn(response);

        ProductResponse result = productService.update(id, request);

        assertThat(result).isEqualTo(response);
        verify(productRepository).findById(id);
        verify(productRepository).save(entity);
        verify(productMapper).toResponseDto(entity);
        verifyNoInteractions(categoryService);
        verify(productMapper).updateEntityFromDto(request, entity);
        verify(productRepository, never()).existsBySkuIgnoreCase(any());
    }

    @Test
    void shouldUpdateProductSku() {
        Long id = 1L;
        ProductRequest request = ProductTestData.request("brp-eng-002");
        Product entity = ProductTestData.entityBuilder().id(1L).build();
        ProductResponse response = ProductTestData.response();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(productRepository.existsBySkuIgnoreCase("BRP-ENG-002")).thenReturn(false);
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponseDto(entity)).thenReturn(response);

        ProductResponse result = productService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getSku()).isEqualTo("BRP-ENG-002");
        verify(productRepository).findById(id);
        verify(productRepository).existsBySkuIgnoreCase("BRP-ENG-002");
        verify(productRepository).save(entity);
        verify(productMapper).toResponseDto(entity);
        verifyNoInteractions(categoryService);
        verify(productMapper).updateEntityFromDto(request, entity);
    }

    @Test
    void shouldUpdateProductCategory() {
        Long id = 1L;
        ProductRequest request = new ProductRequest(
                "brp-eng-001",
                "Поршень с шатуном",
                "Оригинальный поршень в сборе",
                0.85, 7.2, 7.2, 5.0,
                "Новая категория"
        );

        Product entity = ProductTestData.entityBuilder().id(1L).build();
        ProductResponse response = ProductTestData.response();
        Category newCategory = Category.builder().name("Новая категория").build();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(categoryService.getCategoryEntityByCategoryName("Новая категория")).thenReturn(newCategory);
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponseDto(entity)).thenReturn(response);

        ProductResponse result = productService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getCategory().getName()).isEqualTo("Новая категория");
        verify(productRepository).findById(id);
        verify(categoryService).getCategoryEntityByCategoryName("Новая категория");
        verify(productRepository).save(entity);
        verify(productMapper).toResponseDto(entity);
        verify(productMapper).updateEntityFromDto(request, entity);
        verify(productRepository, never()).existsBySkuIgnoreCase(any());
    }

    @Test
    void shouldThrowWhenSkuAlreadyExistOnUpdate() {
        Long id = 1L;
        ProductRequest request = ProductTestData.request("BRP-ENG-002");
        Product entity = ProductTestData.entityBuilder().id(1L).build();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(productRepository.existsBySkuIgnoreCase("BRP-ENG-002")).thenReturn(true);

        assertThatThrownBy(() -> productService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Product with SKU '%s' already exists", "BRP-ENG-002");

        verify(productRepository).findById(id);
        verify(productRepository).existsBySkuIgnoreCase("BRP-ENG-002");
        verify(productRepository, never()).save(any());
        verifyNoInteractions(categoryService);
    }

    @Test
    void shouldThrowWhenCategoryNotFoundOnUpdate() {
        Long id = 1L;
        ProductRequest request = new ProductRequest(
                "brp-eng-001",
                "Поршень с шатуном",
                "Оригинальный поршень в сборе",
                0.85, 7.2, 7.2, 5.0,
                "Новая категория"
        );
        Product entity = ProductTestData.entityBuilder().id(1L).build();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(categoryService.getCategoryEntityByCategoryName("Новая категория")).thenThrow(new CommonBackendException(
                "Category with name 'Новая категория' not found", HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> productService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Category with name 'Новая категория' not found");

        verify(productRepository).findById(id);
        verify(categoryService).getCategoryEntityByCategoryName("Новая категория");
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productMapper);
    }

    @Test
    void shouldNotUpdateSkuWhenSkuIsNull() {
        Long id = 1L;
        ProductRequest request = ProductTestData.request(null);
        Product entity = ProductTestData.entityBuilder().id(1L).build();
        ProductResponse response = ProductTestData.response();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponseDto(entity)).thenReturn(response);

        ProductResponse result = productService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getSku()).isEqualTo("BRP-ENG-001");
        verify(productRepository).findById(id);
        verify(productRepository).save(entity);
        verify(productMapper).toResponseDto(entity);
        verifyNoInteractions(categoryService);
        verify(productMapper).updateEntityFromDto(request, entity);
        verify(productRepository, never()).existsBySkuIgnoreCase(any());
    }

    @Test
    void shouldNotUpdateCategoryWhenCategoryNameIsNull() {
        Long id = 1L;
        ProductRequest request = new ProductRequest(
                "brp-eng-001",
                "Поршень с шатуном",
                "Оригинальный поршень в сборе",
                0.85, 7.2, 7.2, 5.0,
                null
        );
        Product entity = ProductTestData.entityBuilder().id(1L).build();
        ProductResponse response = ProductTestData.response();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponseDto(entity)).thenReturn(response);

        ProductResponse result = productService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getCategory().getName()).isEqualTo("Двигатель и топливная система");
        verify(productRepository).findById(id);
        verify(productRepository).save(entity);
        verify(productMapper).toResponseDto(entity);
        verifyNoInteractions(categoryService);
        verify(productMapper).updateEntityFromDto(request, entity);
        verify(productRepository, never()).existsBySkuIgnoreCase(any());
    }

    @Test
    void shouldUpdateProductSkuAndCategory() {
        Long id = 1L;
        ProductRequest request = new ProductRequest(
                "brp-eng-002",
                "Поршень с шатуном",
                "Оригинальный поршень в сборе",
                0.85, 7.2, 7.2, 5.0,
                "Новая категория"
        );
        Product entity = ProductTestData.entityBuilder().id(1L).build();
        Category newCategory = Category.builder().name("Новая категория").build();
        ProductResponse response = ProductTestData.response();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(productRepository.existsBySkuIgnoreCase("BRP-ENG-002")).thenReturn(false);
        when(categoryService.getCategoryEntityByCategoryName("Новая категория")).thenReturn(newCategory);
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponseDto(entity)).thenReturn(response);

        ProductResponse result = productService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getSku()).isEqualTo("BRP-ENG-002");
        assertThat(entity.getCategory().getName()).isEqualTo("Новая категория");
        verify(productRepository).findById(id);
        verify(productRepository).save(entity);
        verify(productRepository).existsBySkuIgnoreCase("BRP-ENG-002");
        verify(categoryService).getCategoryEntityByCategoryName(request.categoryName());
        verify(productMapper).updateEntityFromDto(request, entity);
        verify(productMapper).toResponseDto(entity);
    }

    @Test
    void shouldDeleteProductById() {
        Long id = 1L;
        Product entity = ProductTestData.entityBuilder().id(1L).build();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(productRepository.save(entity)).thenReturn(entity);

        productService.delete(id);

        assertThat(entity.getIsActive()).isFalse();
        verify(productRepository).findById(id);
        verify(productRepository).save(entity);
    }

    @Test
    void shouldThrowWhenProductNotFoundOnDelete() {
        Long id = 99L;

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Product with id: %s not found", id);

        verify(productRepository).findById(id);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenProductAlreadyDeleted() {
        Long id = 1L;
        Product entity = ProductTestData.entityBuilder().id(1L).isActive(false).build();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> productService.delete(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Product is already deleted");

        verify(productRepository).findById(id);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldActivateProductById() {
        Long id = 1L;
        Product entity = ProductTestData.entityBuilder().id(1L).isActive(false).build();
        ProductResponse response = ProductTestData.response();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponseDto(entity)).thenReturn(response);

        ProductResponse result = productService.activate(id);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getIsActive()).isTrue();
        verify(productRepository).findById(id);
        verify(productRepository).save(entity);
        verify(productMapper).toResponseDto(entity);
    }

    @Test
    void shouldThrowWhenProductNotFoundOnActivate() {
        Long id = 99L;

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.activate(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Product with id: %s not found", id);

        verify(productRepository).findById(id);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenProductAlreadyActivated() {
        Long id = 1L;
        Product entity = ProductTestData.entityBuilder().id(1L).isActive(true).build();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> productService.activate(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Product is already active");

        verify(productRepository).findById(id);
        verify(productRepository, never()).save(any());
    }
}