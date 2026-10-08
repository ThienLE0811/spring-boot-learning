package com.example.crudapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload cho PUT /api/v1/users/me/password — nguoi dung tu doi mat khau cua minh.
 *
 * Bat buoc co currentPassword: token co the bi danh cap hoac may tinh bi bo quen chua
 * dang xuat, doi hoi mat khau cu lam cho ke chiem duoc phien khong the khoa chu that ra.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "currentPassword khong duoc de trong")
        String currentPassword,

        @NotBlank(message = "newPassword khong duoc de trong")
        @Size(min = 8, max = 72, message = "newPassword tu 8 den 72 ky tu")
        String newPassword
) {
}
