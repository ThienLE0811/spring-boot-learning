package com.example.crudapi.dto;

import com.example.crudapi.entity.Category;

/**
 * Ban rut gon cua category de nhung vao ProductResponse.
 *
 * Khong dung thang CategoryResponse: payload cua product khong can createdAt/updatedAt
 * cua category, va long nhau DTO "day du" se lam response phinh ra khi quan he nhieu tang.
 */
public record CategorySummary(
        Long id,
        String name
) {

    /** Tra ve null neu product chua duoc gan category. */
    public static CategorySummary from(Category category) {
        return category == null ? null : new CategorySummary(category.getId(), category.getName());
    }
}
