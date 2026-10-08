package com.example.crudapi.web;

import com.example.crudapi.config.SecurityConfig;
import com.example.crudapi.dto.ChangePasswordRequest;
import com.example.crudapi.dto.PageResponse;
import com.example.crudapi.dto.ResetPasswordRequest;
import com.example.crudapi.dto.UserRequest;
import com.example.crudapi.dto.UserResponse;
import com.example.crudapi.entity.Role;
import com.example.crudapi.exception.GlobalExceptionHandler;
import com.example.crudapi.security.CustomUserDetailsService;
import com.example.crudapi.security.JwtService;
import com.example.crudapi.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chi test phan quyen, qua SecurityFilterChain that.
 *
 * Trong tam la cap rule trong SecurityConfig:
 *     /api/v1/users/me, /api/v1/users/me/**  -> .authenticated()
 *     /api/v1/users/**                       -> .hasRole("ADMIN")
 * Thu tu nay quan trong: dao lai thi USER mat quyen xem thong tin cua chinh minh.
 *
 * JwtService/CustomUserDetailsService duoc mock vi @WebMvcTest tu nap
 * JwtAuthenticationFilter (la mot Filter) va can 2 bean do de khoi tao, du khong
 * request nao o day gui JWT.
 */
@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class UserSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static UserResponse response(Long id, String username, Role role) {
        return new UserResponse(id, username, role, Instant.now(), Instant.now());
    }

    // ----- /users: chi ADMIN -----

    @Test
    void list_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void list_shouldReturn403_whenRoleUser() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_shouldReturn200_whenRoleAdmin() throws Exception {
        when(userService.search(eq(null), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(response(1L, "admin", Role.ADMIN)), 0, 20, 1, 1, true));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getById_shouldReturn403_whenRoleUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_shouldReturn403_whenRoleUser() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserRequest("thien", "matkhau123", Role.USER))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_shouldReturn403_whenRoleUser() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", 2L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER khong duoc reset mat khau cua nguoi khac")
    @WithMockUser(roles = "USER")
    void resetPassword_shouldReturn403_whenRoleUser() throws Exception {
        mockMvc.perform(put("/api/v1/users/{id}/password", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest("matkhaumoi"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_shouldReturn204_whenRoleAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", 2L))
                .andExpect(status().isNoContent());
    }

    // ----- /users/me: moi nguoi da dang nhap -----

    @Test
    void me_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("USER xem duoc thong tin cua chinh minh (chung minh thu tu matcher dung)")
    @WithMockUser(username = "thien", roles = "USER")
    void me_shouldReturn200_whenRoleUser() throws Exception {
        when(userService.getByUsername("thien")).thenReturn(response(2L, "thien", Role.USER));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("thien"));
    }

    @Test
    @WithMockUser(username = "thien", roles = "USER")
    void changeOwnPassword_shouldReturn204_whenRoleUser() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("matkhau123", "matkhaumoi"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void changeOwnPassword_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("matkhau123", "matkhaumoi"))))
                .andExpect(status().isUnauthorized());
    }
}
