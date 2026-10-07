package com.example.crudapi.service;

import com.example.crudapi.dto.PageResponse;
import com.example.crudapi.dto.ProductRequest;
import com.example.crudapi.dto.ProductResponse;
import com.example.crudapi.entity.Category;
import com.example.crudapi.entity.Product;
import com.example.crudapi.exception.DuplicateResourceException;
import com.example.crudapi.exception.InvalidRequestException;
import com.example.crudapi.exception.ResourceNotFoundException;
import com.example.crudapi.repository.CategoryRepository;
import com.example.crudapi.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test tang service: mock repository, khong can database.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    /** Mac dinh khong gan category -> cac test cu khong phai stub categoryRepository. */
    private static ProductRequest sampleRequest() {
        return sampleRequest(null);
    }

    private static ProductRequest sampleRequest(Long categoryId) {
        return new ProductRequest("SKU-001", "Ban phim co", "mo ta",
                new BigDecimal("1250000.00"), 15, categoryId);
    }

    private static Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
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
        assertThat(response.category()).isNull();
        verify(categoryRepository, never()).findById(any());
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
    @DisplayName("create: gan category khi categoryId hop le")
    void create_shouldAttachCategory_whenCategoryIdValid() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category(2L, "Phu kien may tinh")));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ProductResponse response = productService.create(sampleRequest(2L));

        assertThat(response.category()).isNotNull();
        assertThat(response.category().id()).isEqualTo(2L);
        assertThat(response.category().name()).isEqualTo("Phu kien may tinh");
    }

    @Test
    @DisplayName("create: nem ResourceNotFoundException khi categoryId khong ton tai")
    void create_shouldThrow_whenCategoryNotFound() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(sampleRequest(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("category")
                .hasMessageContaining("99");

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
    @DisplayName("update: categoryId null -> go category dang gan (PUT thay the toan bo resource)")
    void update_shouldDetachCategory_whenCategoryIdNull() {
        Product existing = new Product();
        existing.setId(1L);
        existing.setSku("SKU-001");
        existing.setName("Ten cu");
        existing.setPrice(new BigDecimal("100.00"));
        existing.setQuantity(1);
        existing.setCategory(category(2L, "Phu kien may tinh"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuAndIdNot("SKU-001", 1L)).thenReturn(false);

        ProductResponse response = productService.update(1L, sampleRequest(null));

        assertThat(response.category()).isNull();
        assertThat(existing.getCategory()).isNull();
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode patch(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Product existingProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setSku("SKU-001");
        product.setName("Ban phim co");
        product.setDescription("mo ta cu");
        product.setPrice(new BigDecimal("100.00"));
        product.setQuantity(5);
        product.setCategory(category(2L, "Phu kien may tinh"));
        return product;
    }

    @Test
    @DisplayName("patch: chi cap nhat field co mat trong body, field vang mat giu nguyen")
    void patch_shouldUpdateOnlyProvidedFields() {
        Product existing = existingProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        ProductResponse response = productService.patch(1L, patch("{\"quantity\": 20}"));

        assertThat(response.quantity()).isEqualTo(20);
        assertThat(response.sku()).isEqualTo("SKU-001");
        assertThat(response.name()).isEqualTo("Ban phim co");
        assertThat(response.description()).isEqualTo("mo ta cu");
        assertThat(response.category().id()).isEqualTo(2L);
        verify(productRepository, never()).existsBySkuAndIdNot(any(), any());
    }

    @Test
    @DisplayName("patch: gui description = null -> xoa mo ta")
    void patch_shouldClearDescription_whenExplicitNull() {
        Product existing = existingProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        ProductResponse response = productService.patch(1L, patch("{\"description\": null}"));

        assertThat(response.description()).isNull();
    }

    @Test
    @DisplayName("patch: gui categoryId = null -> go category dang gan")
    void patch_shouldDetachCategory_whenCategoryIdExplicitNull() {
        Product existing = existingProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        ProductResponse response = productService.patch(1L, patch("{\"categoryId\": null}"));

        assertThat(response.category()).isNull();
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("patch: doi categoryId -> gan category moi")
    void patch_shouldAttachNewCategory_whenCategoryIdProvided() {
        Product existing = existingProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category(3L, "Linh kien")));

        ProductResponse response = productService.patch(1L, patch("{\"categoryId\": 3}"));

        assertThat(response.category().id()).isEqualTo(3L);
    }

    @Test
    @DisplayName("patch: categoryId moi khong ton tai -> ResourceNotFoundException")
    void patch_shouldThrow_whenCategoryNotFound() {
        Product existing = existingProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.patch(1L, patch("{\"categoryId\": 99}")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("patch: sku moi da ton tai o product khac -> DuplicateResourceException")
    void patch_shouldThrow_whenSkuDuplicated() {
        Product existing = existingProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuAndIdNot("SKU-999", 1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.patch(1L, patch("{\"sku\": \"SKU-999\"}")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("patch: sku = chuoi rong -> InvalidRequestException")
    void patch_shouldThrow_whenSkuBlank() {
        Product existing = existingProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.patch(1L, patch("{\"sku\": \"  \"}")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("patch: price am -> InvalidRequestException")
    void patch_shouldThrow_whenPriceNegative() {
        Product existing = existingProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.patch(1L, patch("{\"price\": -1}")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("patch: quantity am -> InvalidRequestException")
    void patch_shouldThrow_whenQuantityNegative() {
        Product existing = existingProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.patch(1L, patch("{\"quantity\": -1}")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("patch: khong tim thay product -> ResourceNotFoundException")
    void patch_shouldThrow_whenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.patch(99L, patch("{\"quantity\": 1}")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Khong con phan nhanh theo to hop bo loc: moi truong hop deu di qua
     * findAll(Specification, Pageable). Noi dung predicate duoc kiem rieng o
     * ProductSpecificationsTest.
     */
    @Test
    @DisplayName("search: moi to hop bo loc deu di qua mot query duy nhat")
    @SuppressWarnings("unchecked")
    void search_shouldAlwaysUseSpecificationQuery() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        assertThat(productService.search(null, null, Pageable.unpaged()).content()).isEmpty();
        assertThat(productService.search("chuot", null, Pageable.unpaged()).content()).isEmpty();
        assertThat(productService.search(null, 2L, Pageable.unpaged()).content()).isEmpty();
        assertThat(productService.search("chuot", 2L, Pageable.unpaged()).content()).isEmpty();

        verify(productRepository, times(4)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("search: map ca category vao ket qua")
    @SuppressWarnings("unchecked")
    void search_shouldMapCategoryIntoResponse() {
        Product product = new Product();
        product.setId(1L);
        product.setSku("SKU-001");
        product.setName("Ban phim co");
        product.setPrice(new BigDecimal("1250000.00"));
        product.setQuantity(15);
        product.setCategory(category(2L, "Phu kien may tinh"));

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        PageResponse<ProductResponse> response = productService.search(null, 2L, Pageable.unpaged());

        assertThat(response.content()).singleElement().satisfies(item -> {
            assertThat(item.sku()).isEqualTo("SKU-001");
            assertThat(item.category().id()).isEqualTo(2L);
            assertThat(item.category().name()).isEqualTo("Phu kien may tinh");
        });
    }
}
