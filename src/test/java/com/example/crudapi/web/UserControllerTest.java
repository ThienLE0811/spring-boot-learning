package com.example.crudapi.web;

import com.example.crudapi.dto.ChangePasswordRequest;
import com.example.crudapi.dto.PageResponse;
import com.example.crudapi.dto.ResetPasswordRequest;
import com.example.crudapi.dto.UserRequest;
import com.example.crudapi.dto.UserResponse;
import com.example.crudapi.dto.UserUpdateRequest;
import com.example.crudapi.entity.Role;
import com.example.crudapi.exception.DuplicateResourceException;
import com.example.crudapi.exception.GlobalExceptionHandler;
import com.example.crudapi.exception.InvalidRequestException;
import com.example.crudapi.exception.ResourceInUseException;
import com.example.crudapi.exception.ResourceNotFoundException;
import com.example.crudapi.security.CustomUserDetailsService;
import com.example.crudapi.security.JwtService;
import com.example.crudapi.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test tang web: chi nap MVC layer, service duoc mock -> khong dung toi DB.
 * Tat filter chain (addFilters = false) vi test nay kiem tra anh xa HTTP va ma loi,
 * khong kiem tra phan quyen - phan quyen nam rieng o UserSecurityTest.
 */
@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

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

    /**
     * addFilters = false go bo SecurityContextHolderAwareRequestFilter - filter duy nhat
     * lam cho request.getUserPrincipal() tra ve Authentication, va do la cho Spring MVC
     * lay gia tri cho tham so Authentication cua controller. @WithMockUser khong du vi
     * no chi set SecurityContextHolder. Gan thang principal vao request la cach ro rang nhat.
     *
     * Production khong gap van de nay: /users/me** doi .authenticated() nen handler
     * khong bao gio chay voi Authentication null.
     */
    private static Authentication principal(String username) {
        return new UsernamePasswordAuthenticationToken(username, null, List.of());
    }

    // ----- doc -----

    @Test
    void list_shouldReturn200() throws Exception {
        when(userService.search(eq(null), any(Pageable.class))).thenReturn(
                new PageResponse<>(List.of(response(1L, "admin", Role.ADMIN)), 0, 20, 1, 1, true));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("admin"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("response khong bao gio chua password")
    void getById_shouldNotExposePassword() throws Exception {
        when(userService.getById(1L)).thenReturn(response(1L, "admin", Role.ADMIN));

        mockMvc.perform(get("/api/v1/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void getById_shouldReturn404_whenMissing() throws Exception {
        when(userService.getById(99L)).thenThrow(new ResourceNotFoundException("Khong tim thay user voi id = 99"));

        mockMvc.perform(get("/api/v1/users/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    @DisplayName("/users/me uu tien hon /users/{id} vi segment chu thang bien duong dan")
    void me_shouldReturn200() throws Exception {
        when(userService.getByUsername("thien")).thenReturn(response(2L, "thien", Role.USER));

        mockMvc.perform(get("/api/v1/users/me").principal(principal("thien")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("thien"));
    }

    // ----- tao -----

    @Test
    void create_shouldReturn201WithLocation() throws Exception {
        when(userService.create(any(UserRequest.class))).thenReturn(response(5L, "thien", Role.USER));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserRequest("thien", "matkhau123", Role.USER))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/users/5")))
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @DisplayName("password ngan hon 8 ky tu -> 400 kem chi tiet field")
    void create_shouldReturn400_whenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserRequest("thien", "123", Role.USER))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("role khong thuoc enum -> 400 chu khong phai 500")
    void create_shouldReturn400_whenRoleInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"thien","password":"matkhau123","role":"SUPERADMIN"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request body"));
    }

    @Test
    void create_shouldReturn409_whenUsernameDuplicate() throws Exception {
        when(userService.create(any(UserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username da ton tai: admin"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserRequest("admin", "matkhau123", Role.ADMIN))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate resource"));
    }

    // ----- sua -----

    @Test
    void update_shouldReturn200() throws Exception {
        when(userService.update(eq(2L), any(UserUpdateRequest.class)))
                .thenReturn(response(2L, "thien", Role.ADMIN));

        mockMvc.perform(put("/api/v1/users/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserUpdateRequest("thien", Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void update_shouldReturn409_whenDemotingLastAdmin() throws Exception {
        when(userService.update(eq(1L), any(UserUpdateRequest.class)))
                .thenThrow(new ResourceInUseException("He thong phai con it nhat mot tai khoan ADMIN"));

        mockMvc.perform(put("/api/v1/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserUpdateRequest("admin", Role.USER))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource in use"));
    }

    // ----- mat khau -----

    @Test
    void resetPassword_shouldReturn204() throws Exception {
        mockMvc.perform(put("/api/v1/users/{id}/password", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest("matkhaumoi"))))
                .andExpect(status().isNoContent());

        verify(userService).resetPassword(2L, "matkhaumoi");
    }

    @Test
    void changeOwnPassword_shouldReturn204() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/password")
                        .principal(principal("thien"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("matkhau123", "matkhaumoi"))))
                .andExpect(status().isNoContent());

        verify(userService).changeOwnPassword(eq("thien"), any(ChangePasswordRequest.class));
    }

    @Test
    void changeOwnPassword_shouldReturn400_whenCurrentPasswordWrong() throws Exception {
        doThrow(new InvalidRequestException("Mat khau hien tai khong dung"))
                .when(userService).changeOwnPassword(eq("thien"), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/v1/users/me/password")
                        .principal(principal("thien"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("sai12345", "matkhaumoi"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    // ----- xoa -----

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", 2L).principal(principal("admin")))
                .andExpect(status().isNoContent());

        verify(userService).delete(2L, "admin");
    }

    @Test
    void delete_shouldReturn400_whenDeletingSelf() throws Exception {
        doThrow(new InvalidRequestException("Khong the xoa tai khoan dang dang nhap"))
                .when(userService).delete(1L, "admin");

        mockMvc.perform(delete("/api/v1/users/{id}", 1L).principal(principal("admin")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }
}
