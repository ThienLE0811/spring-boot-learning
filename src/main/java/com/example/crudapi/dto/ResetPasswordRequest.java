package com.example.crudapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload cho PUT /api/v1/users/{id}/password — ADMIN dat lai mat khau ho nguoi khac.
 * Khong co currentPassword vi admin khong biet mat khau cu cua nguoi dung.
 */
public record ResetPasswordRequest(

        @NotBlank(message = "newPassword khong duoc de trong")
        @Size(min = 8, max = 72, message = "newPassword tu 8 den 72 ky tu")
        String newPassword
) {
}
