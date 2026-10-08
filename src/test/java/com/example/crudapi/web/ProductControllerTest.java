package com.example.crudapi.web;

import com.example.crudapi.dto.CategorySummary;
import com.example.crudapi.dto.PageResponse;
import com.example.crudapi.dto.ProductRequest;
import com.example.crudapi.dto.ProductResponse;
import com.example.crudapi.entity.Product;
import com.example.crudapi.exception.GlobalExceptionHandler;
import com.example.crudapi.exception.InvalidRequestException;
import com.example.crudapi.exception.ResourceNotFoundException;
import com.example.crudapi.security.CustomUserDetailsService;
import com.example.crudapi.security.JwtService;
import com.example.crudapi.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test tang web: chi nap MVC layer, service duoc mock -> khong dung toi DB.
 * JwtService/CustomUserDetailsService duoc mock vi WebMvcTest tu dong nap
 * JwtAuthenticationFilter (la mot Filter) va can 2 bean nay de khoi tao no.
 */
@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private ProductService productService;

    @Test
    void getById_shouldReturn200() throws Exception {
        when(productService.getById(1L)).thenReturn(new ProductResponse(
                1L, "SKU-001", "Ban phim co", "mo ta",
                new BigDecimal("1250000.00"), 15, new CategorySummary(2L, "Phu kien may tinh"),
                Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/v1/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.quantity").value(15))
                .andExpect(jsonPath("$.category.id").value(2))
                .andExpect(jsonPath("$.category.name").value("Phu kien may tinh"));
    }

    @Test
    @DisplayName("product khong co category -> field category bi loai khoi JSON (non_null inclusion)")
    void getById_shouldOmitCategory_whenNull() throws Exception {
        when(productService.getById(1L)).thenReturn(new ProductResponse(
                1L, "SKU-001", "Ban phim co", null,
                new BigDecimal("1250000.00"), 15, null, Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/v1/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").doesNotExist());
    }

    @Test
    void getById_shouldReturn404_whenMissing() throws Exception {
        when(productService.getById(99L)).thenThrow(new ResourceNotFoundException("Khong tim thay product voi id = 99"));

        mockMvc.perform(get("/api/v1/products/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    void create_shouldReturn400_whenPayloadInvalid() throws Exception {
        ProductRequest invalid = new ProductRequest("", "", null, new BigDecimal("-1"), -5, 0L);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sku").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.quantity").exists())
                .andExpect(jsonPath("$.errors.categoryId").exists());
    }

    @Test
    void create_shouldReturn201WithLocation() throws Exception {
        ProductRequest request = new ProductRequest("SKU-010", "San pham moi", null,
                new BigDecimal("1000.00"), 3, 2L);
        when(productService.create(any(ProductRequest.class))).thenReturn(new ProductResponse(
                10L, "SKU-010", "San pham moi", null,
                new BigDecimal("1000.00"), 3, new CategorySummary(2L, "Phu kien may tinh"),
                Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }


    @Test
    void list_shouldReturn400_whenSortPropertyInvalid() throws Exception {
        when(productService.search(any(), any(), any())).thenThrow(new PropertyReferenceException(
                "[]", TypeInformation.of(Product.class), List.of()));

        mockMvc.perform(get("/api/v1/products").param("sort", "[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid sort property"));
    }

    @Test
    @DisplayName("list: truyen ca keyword lan categoryId xuong service")
    void list_shouldPassBothFiltersToService() throws Exception {
        when(productService.search(eq("chuot"), eq(2L), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "chuot")
                        .param("categoryId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(productService).search(eq("chuot"), eq(2L), any());
    }

    @Test
    @DisplayName("patch: tra ve 200 voi resource da cap nhat")
    void patch_shouldReturn200() throws Exception {
        when(productService.patch(eq(1L), any())).thenReturn(new ProductResponse(
                1L, "SKU-001", "Ban phim co", "mo ta",
                new BigDecimal("1250000.00"), 20, new CategorySummary(2L, "Phu kien may tinh"),
                Instant.now(), Instant.now()));

        mockMvc.perform(patch("/api/v1/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(20));
    }

    @Test
    @DisplayName("patch: product khong ton tai -> 404")
    void patch_shouldReturn404_whenMissing() throws Exception {
        when(productService.patch(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Khong tim thay product voi id = 99"));

        mockMvc.perform(patch("/api/v1/products/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 20}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("patch: gia tri khong hop le -> 400")
    void patch_shouldReturn400_whenValueInvalid() throws Exception {
        when(productService.patch(eq(1L), any()))
                .thenThrow(new InvalidRequestException("quantity phai >= 0"));

        mockMvc.perform(patch("/api/v1/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    @DisplayName("list: categoryId khong phai so -> 400 chu khong phai 500")
    void list_shouldReturn400_whenCategoryIdNotNumeric() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("categoryId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid parameter type"))
                .andExpect(jsonPath("$.detail").value("Tham so 'categoryId' khong dung dinh dang"));
    }
}
