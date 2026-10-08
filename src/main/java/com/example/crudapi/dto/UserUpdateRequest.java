package com.example.crudapi.dto;

import com.example.crudapi.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload cho PUT /api/v1/users/{id}.
 *
 * Co y KHONG co password: PUT thay the toan bo resource, neu password nam trong day
 * thi moi lan chi muon doi role cung phai gui kem mat khau, quen gui la mat khau bi
 * ghi de. Doi mat khau co endpoint rieng.
 */
public record UserUpdateRequest(

        @NotBlank(message = "username khong duoc de trong")
        @Size(min = 3, max = 200, message = "username tu 3 den 200 ky tu")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "username chi gom chu, so, '.', '-' va '_'")
        String username,

        @NotNull(message = "role khong duoc null")
        Role role
) {
}
