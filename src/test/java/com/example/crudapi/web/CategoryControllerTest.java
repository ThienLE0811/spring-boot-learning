package com.example.crudapi.web;

import com.example.crudapi.dto.CategoryRequest;
import com.example.crudapi.dto.CategoryResponse;
import com.example.crudapi.exception.GlobalExceptionHandler;
import com.example.crudapi.exception.ResourceInUseException;
import com.example.crudapi.exception.ResourceNotFoundException;
import com.example.crudapi.security.CustomUserDetailsService;
import com.example.crudapi.security.JwtService;
import com.example.crudapi.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test tang web cho Category: chi nap MVC layer, service duoc mock -> khong dung toi DB.
 * Tat filter chain cua Spring Security (addFilters = false) vi test nay khong nham kiem tra
 * security - quyen ADMIN cho POST/DELETE duoc kiem tra rieng o CategorySecurityTest.
 * JwtService/CustomUserDetailsService van phai mock vi WebMvcTest tu dong nap
 * JwtAuthenticationFilter (la mot Filter) va can 2 bean nay de khoi tao no.
 */
@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getById_shouldReturn200() throws Exception {
        when(categoryService.getById(1L)).thenReturn(new CategoryResponse(
                1L, "Phu kien may tinh", "Ban phim, chuot", Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/v1/categories/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Phu kien may tinh"));
    }

    @Test
    void getById_shouldReturn404_whenMissing() throws Exception {
        when(categoryService.getById(99L))
                .thenThrow(new ResourceNotFoundException("Khong tim thay category voi id = 99"));

        mockMvc.perform(get("/api/v1/categories/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    void create_shouldReturn400_whenNameBlank() throws Exception {
        CategoryRequest invalid = new CategoryRequest("  ", null);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void create_shouldReturn201WithLocation() throws Exception {
        CategoryRequest request = new CategoryRequest("Man hinh", "Man hinh may tinh");
        when(categoryService.create(any(CategoryRequest.class))).thenReturn(new CategoryResponse(
                5L, "Man hinh", "Man hinh may tinh", Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/categories/5"))
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete: category con product tham chieu -> 409 kem ly do cu the")
    void delete_shouldReturn409_whenStillReferenced() throws Exception {
        doThrow(new ResourceInUseException("Khong the xoa category dang duoc product su dung: Man hinh"))
                .when(categoryService).delete(1L);

        mockMvc.perform(delete("/api/v1/categories/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource in use"))
                .andExpect(jsonPath("$.detail").value(
                        "Khong the xoa category dang duoc product su dung: Man hinh"));
    }
}
