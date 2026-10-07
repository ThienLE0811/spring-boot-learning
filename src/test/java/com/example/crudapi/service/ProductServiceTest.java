package com.example.crudapi.service;

import com.example.crudapi.dto.PageResponse;
import com.example.crudapi.dto.ProductRequest;
import com.example.crudapi.dto.ProductResponse;
import com.example.crudapi.entity.Product;
import com.example.crudapi.exception.DuplicateResourceException;
import com.example.crudapi.exception.ResourceNotFoundException;
import com.example.crudapi.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test tang service: mock repository, khong can database.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private static ProductRequest sampleRequest() {
        return new ProductRequest("SKU-001", "Ban phim co", "mo ta",
                new BigDecimal("1250000.00"), 15);
    }

    @Test
    @DisplayName("create: luu thanh cong khi sku chua ton tai")
    void create_shouldPersist_whenSkuIsAvailable() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ProductResponse response = productService.create(sampleRequest());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.sku()).isEqualTo("SKU-001");
        assertThat(response.quantity()).isEqualTo(15);
    }

    @Test
    @DisplayName("create: nem DuplicateResourceException khi sku da ton tai")
    void create_shouldThrow_whenSkuDuplicated() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(sampleRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU-001");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("getById: nem ResourceNotFoundException khi khong co ban ghi")
    void getById_shouldThrow_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("update: cap nhat field tren entity dang managed")
    void update_shouldMutateManagedEntity() {
        Product existing = new Product();
        existing.setId(1L);
        existing.setSku("SKU-001");
        existing.setName("Ten cu");
        existing.setPrice(new BigDecimal("100.00"));
        existing.setQuantity(1);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuAndIdNot("SKU-001", 1L)).thenReturn(false);

        ProductResponse response = productService.update(1L, sampleRequest());

        assertThat(response.name()).isEqualTo("Ban phim co");
        assertThat(existing.getPrice()).isEqualByComparingTo("1250000.00");
    }

    @Test
    @DisplayName("search: khong co keyword -> dung findAll, khong dung derived query")
    void search_shouldUseFindAll_whenKeywordBlank() {
        when(productRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        PageResponse<ProductResponse> response = productService.search("  ", Pageable.unpaged());

        assertThat(response.content()).isEmpty();
        verify(productRepository).findAll(any(Pageable.class));
        verify(productRepository, never())
                .findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(any(), any(), any());
    }

    @Test
    @DisplayName("search: co keyword -> loc theo name hoac sku, da trim")
    void search_shouldFilter_whenKeywordPresent() {
        when(productRepository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
                eq("chuot"), eq("chuot"), any(Pageable.class))).thenReturn(Page.empty());

        productService.search("  chuot  ", Pageable.unpaged());

        verify(productRepository).findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
                eq("chuot"), eq("chuot"), any(Pageable.class));
        verify(productRepository, never()).findAll(any(Pageable.class));
    }
}
