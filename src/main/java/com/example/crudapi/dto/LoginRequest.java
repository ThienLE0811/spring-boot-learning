package com.example.crudapi.dto;

import jakarta.validation.constraints.NotBlank;

/** Payload cho POST /api/v1/auth/login. */
public record LoginRequest(

        @NotBlank(message = "username khong duoc de trong")
        String username,

        @NotBlank(message = "password khong duoc de trong")
        String password
) {
}
