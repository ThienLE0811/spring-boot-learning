package com.example.crudapi.service;

import com.example.crudapi.dto.CategoryRequest;
import com.example.crudapi.dto.CategoryResponse;
import com.example.crudapi.entity.Category;
import com.example.crudapi.exception.DuplicateResourceException;
import com.example.crudapi.exception.ResourceInUseException;
import com.example.crudapi.exception.ResourceNotFoundException;
import com.example.crudapi.repository.CategoryRepository;
import com.example.crudapi.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private static CategoryRequest sampleRequest() {
        return new CategoryRequest("Phu kien may tinh", "  Ban phim, chuot  ");
    }

    private static Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    @Test
    @DisplayName("create: luu thanh cong va trim description")
    void create_shouldPersist_whenNameAvailable() {
        when(categoryRepository.existsByName("Phu kien may tinh")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CategoryResponse response = categoryService.create(sampleRequest());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Phu kien may tinh");
        assertThat(response.description()).isEqualTo("Ban phim, chuot");
    }

    @Test
    @DisplayName("create: nem DuplicateResourceException khi ten da ton tai")
    void create_shouldThrow_whenNameDuplicated() {
        when(categoryRepository.existsByName("Phu kien may tinh")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(sampleRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Phu kien may tinh");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: description rong -> luu thanh null")
    void update_shouldNormalizeBlankDescriptionToNull() {
        Category existing = category(1L, "Ten cu");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndIdNot("Man hinh", 1L)).thenReturn(false);

        CategoryResponse response = categoryService.update(1L, new CategoryRequest("Man hinh", "   "));

        assertThat(response.name()).isEqualTo("Man hinh");
        assertThat(response.description()).isNull();
        assertThat(existing.getName()).isEqualTo("Man hinh");
    }

    @Test
    @DisplayName("getById: nem ResourceNotFoundException khi khong co ban ghi")
    void getById_shouldThrow_whenNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("delete: xoa duoc khi khong con product tham chieu")
    void delete_shouldRemove_whenNotReferenced() {
        Category existing = category(1L, "Man hinh");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).delete(existing);
    }

    @Test
    @DisplayName("delete: nem ResourceInUseException khi con product tham chieu")
    void delete_shouldThrow_whenStillReferenced() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Man hinh")));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("Man hinh");

        verify(categoryRepository, never()).delete(any());
    }
}
