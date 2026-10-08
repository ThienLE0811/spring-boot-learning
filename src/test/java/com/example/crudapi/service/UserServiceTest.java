package com.example.crudapi.service;

import com.example.crudapi.dto.ChangePasswordRequest;
import com.example.crudapi.dto.PageResponse;
import com.example.crudapi.dto.UserRequest;
import com.example.crudapi.dto.UserResponse;
import com.example.crudapi.dto.UserUpdateRequest;
import com.example.crudapi.entity.Role;
import com.example.crudapi.entity.User;
import com.example.crudapi.exception.DuplicateResourceException;
import com.example.crudapi.exception.InvalidRequestException;
import com.example.crudapi.exception.ResourceInUseException;
import com.example.crudapi.exception.ResourceNotFoundException;
import com.example.crudapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test business logic bang Mockito thuan: khong Spring context, khong DB.
 * PasswordEncoder duoc mock nen hash la chuoi gia ("HASH_..."), test khong
 * phu thuoc vao thuat toan BCrypt - chi kiem tra service co GOI encode hay khong.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Pageable DEFAULT_PAGE = PageRequest.of(0, 20, Sort.by("username"));

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private static User user(Long id, String username, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setPassword("HASH_CU");
        return user;
    }

    // ----- create -----

    @Test
    @DisplayName("create: mat khau phai duoc hash truoc khi luu")
    void create_shouldHashPasswordBeforeSaving() {
        when(userRepository.existsByUsername("thien")).thenReturn(false);
        when(passwordEncoder.encode("matkhau123")).thenReturn("HASH_MOI");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.create(new UserRequest("thien", "matkhau123", Role.USER));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("HASH_MOI");
        assertThat(captor.getValue().getPassword()).isNotEqualTo("matkhau123");
    }

    @Test
    void create_shouldThrowDuplicate_whenUsernameExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(new UserRequest("admin", "matkhau123", Role.USER)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("create: username duoc trim truoc khi kiem tra trung")
    void create_shouldTrimUsername() {
        when(userRepository.existsByUsername("thien")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("HASH_MOI");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.create(new UserRequest("  thien  ", "matkhau123", Role.USER));

        assertThat(response.username()).isEqualTo("thien");
    }

    // ----- update -----

    @Test
    void update_shouldThrowDuplicate_whenUsernameTakenByAnother() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "thien", Role.USER)));
        when(userRepository.existsByUsernameAndIdNot("admin", 2L)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(2L, new UserUpdateRequest("admin", Role.USER)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("update: khong cho ha quyen ADMIN cuoi cung")
    void update_shouldThrowResourceInUse_whenDemotingLastAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", Role.ADMIN)));
        when(userRepository.existsByUsernameAndIdNot("admin", 1L)).thenReturn(false);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.update(1L, new UserUpdateRequest("admin", Role.USER)))
                .isInstanceOf(ResourceInUseException.class);
    }

    @Test
    @DisplayName("update: cho ha quyen khi van con ADMIN khac")
    void update_shouldAllowDemotion_whenAnotherAdminExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", Role.ADMIN)));
        when(userRepository.existsByUsernameAndIdNot("admin", 1L)).thenReturn(false);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);

        UserResponse response = userService.update(1L, new UserUpdateRequest("admin", Role.USER));

        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("update: giu nguyen role ADMIN thi khong can dem admin")
    void update_shouldNotCountAdmins_whenRoleUnchanged() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", Role.ADMIN)));
        when(userRepository.existsByUsernameAndIdNot("admin2", 1L)).thenReturn(false);

        userService.update(1L, new UserUpdateRequest("admin2", Role.ADMIN));

        verify(userRepository, never()).countByRole(any());
    }

    @Test
    @DisplayName("update: khong goi save() vi entity dang managed")
    void update_shouldNotCallSave() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "thien", Role.USER)));
        when(userRepository.existsByUsernameAndIdNot("thien2", 2L)).thenReturn(false);

        userService.update(2L, new UserUpdateRequest("thien2", Role.USER));

        verify(userRepository, never()).save(any(User.class));
    }

    // ----- delete -----

    @Test
    @DisplayName("delete: khong cho tu xoa tai khoan dang dang nhap")
    void delete_shouldThrowInvalidRequest_whenDeletingSelf() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", Role.ADMIN)));

        assertThatThrownBy(() -> userService.delete(1L, "admin"))
                .isInstanceOf(InvalidRequestException.class);

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("delete: khong cho xoa ADMIN cuoi cung")
    void delete_shouldThrowResourceInUse_whenDeletingLastAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", Role.ADMIN)));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.delete(1L, "admin2"))
                .isInstanceOf(ResourceInUseException.class);

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("delete: user thuong khong can kiem tra so luong admin")
    void delete_shouldDeleteNormalUser() {
        User target = user(2L, "thien", Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        userService.delete(2L, "admin");

        verify(userRepository).delete(target);
        verify(userRepository, never()).countByRole(any());
    }

    // ----- doi mat khau -----

    @Test
    void changeOwnPassword_shouldThrow_whenCurrentPasswordWrong() {
        when(userRepository.findByUsername("thien")).thenReturn(Optional.of(user(2L, "thien", Role.USER)));
        when(passwordEncoder.matches("sai12345", "HASH_CU")).thenReturn(false);

        assertThatThrownBy(() -> userService.changeOwnPassword("thien",
                new ChangePasswordRequest("sai12345", "matkhaumoi")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("doi mat khau: mat khau moi phai khac mat khau cu")
    void changeOwnPassword_shouldThrow_whenNewPasswordSameAsCurrent() {
        when(userRepository.findByUsername("thien")).thenReturn(Optional.of(user(2L, "thien", Role.USER)));
        when(passwordEncoder.matches("matkhau123", "HASH_CU")).thenReturn(true);

        assertThatThrownBy(() -> userService.changeOwnPassword("thien",
                new ChangePasswordRequest("matkhau123", "matkhau123")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void changeOwnPassword_shouldEncodeNewPassword() {
        User target = user(2L, "thien", Role.USER);
        when(userRepository.findByUsername("thien")).thenReturn(Optional.of(target));
        when(passwordEncoder.matches("matkhau123", "HASH_CU")).thenReturn(true);
        when(passwordEncoder.matches("matkhaumoi", "HASH_CU")).thenReturn(false);
        when(passwordEncoder.encode("matkhaumoi")).thenReturn("HASH_MOI");

        userService.changeOwnPassword("thien", new ChangePasswordRequest("matkhau123", "matkhaumoi"));

        assertThat(target.getPassword()).isEqualTo("HASH_MOI");
    }

    @Test
    @DisplayName("admin reset ho: khong can mat khau cu")
    void resetPassword_shouldEncodeWithoutCheckingCurrent() {
        User target = user(2L, "thien", Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(passwordEncoder.encode("matkhaumoi")).thenReturn("HASH_MOI");

        userService.resetPassword(2L, "matkhaumoi");

        assertThat(target.getPassword()).isEqualTo("HASH_MOI");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    // ----- doc -----

    @Test
    void getById_shouldThrowNotFound_whenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByUsername_shouldThrowNotFound_whenMissing() {
        when(userRepository.findByUsername("ai-do")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByUsername("ai-do"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void search_shouldMapToResponse() {
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user(1L, "admin", Role.ADMIN)), DEFAULT_PAGE, 1));

        PageResponse<UserResponse> result = userService.search(null, DEFAULT_PAGE);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).username()).isEqualTo("admin");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("search: tu khoa duoc trim va dung derived query rieng")
    void search_shouldUseKeywordQuery_whenKeywordPresent() {
        when(userRepository.findByUsernameContainingIgnoreCase("ad", DEFAULT_PAGE))
                .thenReturn(new PageImpl<>(List.of(user(1L, "admin", Role.ADMIN)), DEFAULT_PAGE, 1));

        PageResponse<UserResponse> result = userService.search("  ad  ", DEFAULT_PAGE);

        assertThat(result.content()).hasSize(1);
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("search: khong cho sap xep theo password")
    void search_shouldThrowInvalidRequest_whenSortingByPassword() {
        Pageable sortByPassword = PageRequest.of(0, 20, Sort.by("password"));

        assertThatThrownBy(() -> userService.search(null, sortByPassword))
                .isInstanceOf(InvalidRequestException.class);
    }
}
