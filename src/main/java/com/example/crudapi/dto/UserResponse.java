package com.example.crudapi.dto;

import com.example.crudapi.entity.Role;
import com.example.crudapi.entity.User;

import java.time.Instant;

/**
 * Day la ly do quan trong nhat de tach DTO khoi entity: User co field password
 * (BCrypt hash), record nay khong co, nen khong co duong nao de hash lo ra JSON
 * du lap trinh vien co so y. Tra thang entity la mat ca hash cho client.
 */
public record UserResponse(
        Long id,
        String username,
        Role role,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
