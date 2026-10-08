package com.example.crudapi.web;

import com.example.crudapi.dto.ChangePasswordRequest;
import com.example.crudapi.dto.PageResponse;
import com.example.crudapi.dto.ResetPasswordRequest;
import com.example.crudapi.dto.UserRequest;
import com.example.crudapi.dto.UserResponse;
import com.example.crudapi.dto.UserUpdateRequest;
import com.example.crudapi.service.UserService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Phan quyen duoc khai bao tap trung o SecurityConfig theo URL, khong rai rac
 * bang @PreAuthorize tren tung method: /users/me** cho moi nguoi da dang nhap,
 * phan con lai chi ADMIN.
 *
 * Day cung la noi duy nhat cham vao Authentication; service chi nhan username
 * duoi dang String de khong phu thuoc vao tang web.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** GET /api/v1/users?keyword=abc&page=0&size=20&sort=username,asc */
    @GetMapping
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) String keyword,
            @ParameterObject @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC) Pageable pageable) {
        return userService.search(keyword, pageable);
    }

    /**
     * Phai khai bao TRUOC /{id}? Khong can: Spring uu tien segment chu (literal)
     * hon bien duong dan, nen /users/me luon vao day chu khong roi vao /users/{id}.
     * Dat canh nhau o day chi de de doc.
     */
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.getByUsername(authentication.getName());
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeOwnPassword(Authentication authentication,
                                   @Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse created = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    /** ADMIN dat lai mat khau ho nguoi khac. Tra 204 vi khong co gi hop ly de tra ve. */
    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        userService.delete(id, authentication.getName());
    }
}
