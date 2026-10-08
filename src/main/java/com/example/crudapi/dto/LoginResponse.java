package com.example.crudapi.dto;

/** Response cho POST /api/v1/auth/login. */
public record LoginResponse(String accessToken, String tokenType) {

    public static LoginResponse of(String accessToken) {
        return new LoginResponse(accessToken, "Bearer");
    }
}
