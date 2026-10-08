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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Business logic cho quan ly user.
 *
 * Service KHONG tu doc SecurityContextHolder: username cua nguoi dang goi duoc
 * controller truyen xuong duoi dang tham so. Nho vay service van test duoc bang
 * Mockito thuan, khong phai dung SecurityContext gia trong test.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    /**
     * Danh sach trang: 'password' la field co that nen Spring Data san sang
     * ORDER BY password. Hash khong lot ra response, nhung thu tu sap xep van la
     * mot kenh ro ri thong tin, va khong co ly do nghiep vu nao de sap xep theo no.
     */
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "username", "role", "createdAt", "updatedAt");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResponse<UserResponse> search(String keyword, Pageable pageable) {
        ensureSortable(pageable);

        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<User> page = (normalized == null)
                ? userRepository.findAll(pageable)
                : userRepository.findByUsernameContainingIgnoreCase(normalized, pageable);

        return PageResponse.of(page, UserResponse::from);
    }

    public UserResponse getById(Long id) {
        return UserResponse.from(findOrThrow(id));
    }

    /** Phuc vu GET /api/v1/users/me: chi biet username tu token, khong biet id. */
    public UserResponse getByUsername(String username) {
        return UserResponse.from(findByUsernameOrThrow(username));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username da ton tai: " + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setRole(request.role());
        // Mat khau tho khong bao gio roi xuong DB: hash ngay tai day.
        user.setPassword(passwordEncoder.encode(request.password()));

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findOrThrow(id);

        String username = request.username().trim();
        if (userRepository.existsByUsernameAndIdNot(username, id)) {
            throw new DuplicateResourceException("Username da ton tai: " + username);
        }

        // Kiem tra TRUOC khi setRole. Doi thu tu la sai: Hibernate tu flush thay doi
        // dang cho xuong DB truoc khi chay query, nen countByRole se dem tren du lieu
        // da bi ha quyen va khong con phat hien duoc day la admin cuoi cung.
        if (user.getRole() == Role.ADMIN && request.role() != Role.ADMIN) {
            ensureNotLastAdmin();
        }

        user.setUsername(username);
        user.setRole(request.role());

        // Entity dang managed -> Hibernate tu flush khi commit, khong can save().
        return UserResponse.from(user);
    }

    /**
     * @param currentUsername username cua nguoi dang goi, lay tu token
     */
    @Transactional
    public void delete(Long id, String currentUsername) {
        User user = findOrThrow(id);

        // Khong co rang buoc nay thi mot admin co the tu xoa minh va mat quyen ngay lap tuc.
        if (user.getUsername().equals(currentUsername)) {
            throw new InvalidRequestException("Khong the xoa tai khoan dang dang nhap");
        }

        if (user.getRole() == Role.ADMIN) {
            ensureNotLastAdmin();
        }

        userRepository.delete(user);
    }

    /** ADMIN dat lai mat khau ho: khong can biet mat khau cu. */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = findOrThrow(id);
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    /** Nguoi dung tu doi mat khau: phai chung minh biet mat khau hien tai. */
    @Transactional
    public void changeOwnPassword(String username, ChangePasswordRequest request) {
        User user = findByUsernameOrThrow(username);

        // Phai dung matches(): BCrypt tron salt ngau nhien vao moi lan hash, nen
        // encode(cungMotChuoi) ra ket qua khac nhau. So sanh hash bang equals() luon sai.
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidRequestException("Mat khau hien tai khong dung");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new InvalidRequestException("Mat khau moi phai khac mat khau hien tai");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    /**
     * Rang buoc toan he thong (khac voi validate tren mot ban ghi): luon phai con it
     * nhat mot ADMIN, neu khong se khong ai con quyen quan tri va he thong tu khoa minh.
     */
    private void ensureNotLastAdmin() {
        if (userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new ResourceInUseException("He thong phai con it nhat mot tai khoan ADMIN");
        }
    }

    private void ensureSortable(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidRequestException("Khong the sap xep theo truong: " + order.getProperty());
            }
        });
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user voi id = " + id));
    }

    private User findByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user: " + username));
    }
}
