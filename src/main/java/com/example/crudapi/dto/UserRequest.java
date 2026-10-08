package com.example.crudapi.dto;

import com.example.crudapi.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload cho POST /api/v1/users. Chi dung khi TAO user nen moi co password;
 * PUT dung UserUpdateRequest de doi role khong vo tinh reset mat khau.
 */
public record UserRequest(

        @NotBlank(message = "username khong duoc de trong")
        @Size(min = 3, max = 200, message = "username tu 3 den 200 ky tu")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "username chi gom chu, so, '.', '-' va '_'")
        String username,

        /**
         * Gioi han 72: BCrypt chi bam 72 BYTE dau tien, phan du bi bo qua am tham.
         * Chan o day de nguoi dung khong tuong mat khau dai hon la an toan hon.
         */
        @NotBlank(message = "password khong duoc de trong")
        @Size(min = 8, max = 72, message = "password tu 8 den 72 ky tu")
        String password,

        @NotNull(message = "role khong duoc null")
        Role role
) {
}
