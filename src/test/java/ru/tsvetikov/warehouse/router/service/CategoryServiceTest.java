package ru.tsvetikov.warehouse.router.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Category;
import ru.tsvetikov.warehouse.router.model.db.repository.CategoryRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.CategoryRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.CategoryResponse;
import ru.tsvetikov.warehouse.router.model.mapper.CategoryMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldCreateCategory() {
        CategoryRequest request = new CategoryRequest("Electronics", "Description");

        Category entity = Category.builder()
                .name("Electronics")
                .description("Description")
                .build();

        Category saved = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Description")
                .build();

        CategoryResponse response = new CategoryResponse(1L, "Electronics", "Description");

        when(categoryRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(entity)).thenReturn(saved);
        when(categoryMapper.toResponseDto(saved)).thenReturn(response);

        CategoryResponse result = categoryService.create(request);

        assertThat(result).isEqualTo(response);
        verify(categoryRepository).save(entity);
    }

    @Test
    void shouldThrowWhenNameExists() {
        CategoryRequest request = new CategoryRequest("Electronics", "Description");

        when(categoryRepository.existsByNameIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Category with name already exists");

        verifyNoInteractions(categoryMapper);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldNotFormatNameWhenNull() {
        CategoryRequest request = new CategoryRequest(null, "Description");

        Category entity = Category.builder()
                .description("Description")
                .name(null)
                .build();

        Category saved = Category.builder()
                .id(1L)
                .description("Description")
                .name(null)
                .build();

        CategoryResponse response = new CategoryResponse(1L, null, "Description");

        when(categoryRepository.existsByNameIgnoreCase(null)).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(entity)).thenReturn(saved);
        when(categoryMapper.toResponseDto(saved)).thenReturn(response);

        CategoryResponse result = categoryService.create(request);

        assertThat(result).isEqualTo(response);
        verify(categoryRepository).existsByNameIgnoreCase(null);
    }

    @Test
    void shouldNotFormatBlankName() {
        CategoryRequest request = new CategoryRequest("   ", "Description");

        Category entity = Category.builder()
                .description("Description")
                .name("   ")
                .build();

        Category saved = Category.builder()
                .id(1L)
                .description("Description")
                .name("   ")
                .build();

        CategoryResponse response = new CategoryResponse(1L, "   ", "Description");

        when(categoryRepository.existsByNameIgnoreCase("   ")).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(entity)).thenReturn(saved);
        when(categoryMapper.toResponseDto(saved)).thenReturn(response);

        CategoryResponse result = categoryService.create(request);

        assertThat(result).isEqualTo(response);
        verify(categoryRepository).existsByNameIgnoreCase("   ");
    }

    @Test
    void shouldFindCategoryById() {
        Long id = 1L;

        Category entity = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Description")
                .build();

        CategoryResponse response = new CategoryResponse(1L, "Electronics", "Description");

        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));
        when(categoryMapper.toResponseDto(entity)).thenReturn(response);

        CategoryResponse result = categoryService.getById(id);

        assertThat(result).isEqualTo(response);
        verify(categoryMapper).toResponseDto(entity);
        verify(categoryRepository).findById(id);
    }

    @Test
    void shouldThrowWhenCategoryNotFoundOnGetById() {
        Long id = 99L;

        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Category with id: 99 not found");

        verify(categoryRepository).findById(id);
        verify(categoryMapper, never()).toResponseDto(any());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldReturnPageOfCategories() {
        Pageable pageable = PageRequest.of(0, 10);

        Category entity = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Description")
                .build();

        CategoryResponse response = new CategoryResponse(1L, "Electronics", "Description");

        Page<Category> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(categoryRepository.findAllByIsActiveTrue(any(Pageable.class))).thenReturn(page);
        when(categoryMapper.toResponseDto(entity)).thenReturn(response);

        Page<CategoryResponse> result = categoryService.getAll(0, 10, "name", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(categoryRepository).findAllByIsActiveTrue(any(Pageable.class));
        verify(categoryMapper).toResponseDto(entity);
    }

    @Test
    void shouldSearchCategories() {
        String query = "Elect";
        Pageable pageable = PageRequest.of(0, 10);

        Category entity = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Description")
                .build();

        CategoryResponse response = new CategoryResponse(1L, "Electronics", "Description");

        Page<Category> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(categoryRepository.searchActive(eq(query), any(Pageable.class)))
                .thenReturn(page);
        when(categoryMapper.toResponseDto(entity)).thenReturn(response);

        Page<CategoryResponse> result = categoryService.search(
                query, 0, 10, "name", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(categoryRepository).searchActive(eq(query), any(Pageable.class));
    }

    @Test
    void shouldFindCategoryByCategoryName() {
        String categoryName = "Electronics";

        Category entity = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Description")
                .build();

        when(categoryRepository.findByName(categoryName)).thenReturn(Optional.of(entity));

        Category result = categoryService.getCategoryEntityByCategoryName(categoryName);

        assertThat(result).isEqualTo(entity);
        verify(categoryRepository).findByName(categoryName);
    }

    @Test
    void shouldThrowWhenCategoryNameNotFound() {
        String categoryName = "Unknown";

        when(categoryRepository.findByName(categoryName)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryEntityByCategoryName(categoryName))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Category with name 'Unknown' not found");

        verify(categoryRepository).findByName(categoryName);
    }

    @Test
    void shouldUpdateCategoryByCategoryId() {
        Long id = 1L;
        CategoryRequest request = new CategoryRequest("Products", "Description");

        Category entity = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Description")
                .build();

        Category saved = Category.builder()
                .id(1L)
                .name("Products")
                .description("Description")
                .build();

        CategoryResponse response = new CategoryResponse(1L, "Products", "Description");

        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));
        when(categoryRepository.existsByNameIgnoreCase("Products")).thenReturn(false);
        when(categoryRepository.save(entity)).thenReturn(saved);
        when(categoryMapper.toResponseDto(saved)).thenReturn(response);

        CategoryResponse result = categoryService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getName()).isEqualTo("Products");
        verify(categoryRepository).save(entity);
        verify(categoryRepository).existsByNameIgnoreCase("Products");
        verify(categoryMapper).updateEntityFromDto(request, entity);
    }

    @Test
    void shouldThrowWhenUpdatedNameExists() {
        Long id = 1L;
        CategoryRequest request = new CategoryRequest("Products", "Description");

        Category entity = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Description")
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));
        when(categoryRepository.existsByNameIgnoreCase("Products")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("already exists");

        verify(categoryRepository, never()).save(any());
        verify(categoryMapper, never()).updateEntityFromDto(any(), any());
    }

    @Test
    void shouldNotCheckUniquenessWhenNameUnchanged() {
        Long id = 1L;
        CategoryRequest request = new CategoryRequest("Electronics", "New Description");

        Category entity = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Old Description")
                .build();

        Category saved = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("New Description")
                .build();

        CategoryResponse response = new CategoryResponse(1L, "Electronics", "New Description");

        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));
        when(categoryRepository.save(entity)).thenReturn(saved);
        when(categoryMapper.toResponseDto(saved)).thenReturn(response);

        CategoryResponse result = categoryService.update(id, request);

        assertThat(result).isEqualTo(response);

        verify(categoryRepository, never()).existsByNameIgnoreCase(any());
        verify(categoryMapper).updateEntityFromDto(request, entity);
    }

    @Test
    void shouldDeleteCategory() {
        Long id = 1L;

        Category entity = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Description")
                .isActive(true)
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));
        when(categoryRepository.save(entity)).thenReturn(entity);

        categoryService.delete(id);

        assertThat(entity.getIsActive()).isFalse();
        verify(categoryRepository).save(entity);
    }

    @Test
    void shouldThrowWhenCategoryNotFoundOnDelete() {
        Long id = 99L;

        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Category with id: 99 not found");

        verify(categoryRepository).findById(id);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCategoryAlreadyDeleted() {
        Long id = 1L;

        Category entity = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Description")
                .isActive(false)
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> categoryService.delete(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Category is already deleted");

        verify(categoryRepository).findById(id);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldActivateCategory() {
        Long id = 1L;

        Category entity = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Description")
                .isActive(false)
                .build();

        CategoryResponse response = new CategoryResponse(1L, "Electronics", "Description");

        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));
        when(categoryRepository.save(entity)).thenReturn(entity);
        when(categoryMapper.toResponseDto(entity)).thenReturn(response);

        CategoryResponse result = categoryService.activate(id);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getIsActive()).isTrue();

        verify(categoryRepository).findById(id);
        verify(categoryMapper).toResponseDto(entity);
        verify(categoryRepository).save(entity);
    }

    @Test
    void shouldThrowWhenCategoryNotFoundOnActivate() {
        Long id = 99L;

        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.activate(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Category with id: 99 not found");

        verify(categoryRepository).findById(id);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCategoryAlreadyActivated() {
        Long id = 1L;

        Category entity = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Description")
                .isActive(true)
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> categoryService.activate(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Category is already active");

        verify(categoryRepository).findById(id);
        verify(categoryRepository, never()).save(any());
    }
}