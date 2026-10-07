package com.example.crudapi.web;

import com.example.crudapi.dto.ProductRequest;
import com.example.crudapi.dto.ProductResponse;
import com.example.crudapi.entity.Product;
import com.example.crudapi.exception.GlobalExceptionHandler;
import com.example.crudapi.exception.ResourceNotFoundException;
import com.example.crudapi.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test tang web: chi nap MVC layer, service duoc mock -> khong dung toi DB.
 */
@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    void getById_shouldReturn200() throws Exception {
        when(productService.getById(1L)).thenReturn(new ProductResponse(
                1L, "SKU-001", "Ban phim co", "mo ta",
                new BigDecimal("1250000.00"), 15, Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/v1/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.quantity").value(15));
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
        ProductRequest invalid = new ProductRequest("", "", null, new BigDecimal("-1"), -5);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sku").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.quantity").exists());
    }

    @Test
    void create_shouldReturn201WithLocation() throws Exception {
        ProductRequest request = new ProductRequest("SKU-010", "San pham moi", null,
                new BigDecimal("1000.00"), 3);
        when(productService.create(any(ProductRequest.class))).thenReturn(new ProductResponse(
                10L, "SKU-010", "San pham moi", null,
                new BigDecimal("1000.00"), 3, Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }


    @Test
    void list_shouldReturn400_whenSortPropertyInvalid() throws Exception {
        when(productService.search(any(), any())).thenThrow(new PropertyReferenceException(
                "[]", TypeInformation.of(Product.class), List.of()));

        mockMvc.perform(get("/api/v1/products").param("sort", "[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid sort property"));
    }
}
