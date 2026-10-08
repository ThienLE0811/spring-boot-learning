package com.example.crudapi.web;

import com.example.crudapi.config.SecurityConfig;
import com.example.crudapi.dto.CategoryRequest;
import com.example.crudapi.dto.CategoryResponse;
import com.example.crudapi.exception.GlobalExceptionHandler;
import com.example.crudapi.security.CustomUserDetailsService;
import com.example.crudapi.security.JwtService;
import com.example.crudapi.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chi test rieng rang buoc "chi ADMIN duoc tao/xoa category" qua SecurityFilterChain that.
 * JwtService/CustomUserDetailsService duoc mock vi JwtAuthenticationFilter (duoc WebMvcTest
 * tu dong nap do la Filter) can chung de khoi tao, nhung khong request nao o day gui JWT
 * nen chung khong duoc goi toi.
 */
@WebMvcTest(CategoryController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class CategorySecurityTest {

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
    void create_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Man hinh", null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_shouldReturn403_whenRoleUser() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Man hinh", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_shouldReturn201_whenRoleAdmin() throws Exception {
        when(categoryService.create(any(CategoryRequest.class))).thenReturn(new CategoryResponse(
                5L, "Man hinh", null, Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Man hinh", null))))
                .andExpect(status().isCreated());
    }

    @Test
    void delete_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_shouldReturn403_whenRoleUser() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_shouldReturn204_whenRoleAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
