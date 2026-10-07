package com.example.crudapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload cho POST / PUT. Tach khoi entity de khong lo cau truc DB ra ngoai API
 * va de client khong the tu set id / version / createdAt.
 */
public record ProductRequest(

        @NotBlank(message = "sku khong duoc de trong")
        @Size(max = 50, message = "sku toi da 50 ky tu")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "sku chi gom chu, so, '-' va '_'")
        String sku,

        @NotBlank(message = "name khong duoc de trong")
        @Size(max = 200, message = "name toi da 200 ky tu")
        String name,

        @Size(max = 2000, message = "description toi da 2000 ky tu")
        String description,

        @NotNull(message = "price khong duoc null")
        @DecimalMin(value = "0.00", message = "price phai >= 0")
        @Digits(integer = 10, fraction = 2, message = "price toi da 10 chu so phan nguyen va 2 chu so thap phan")
        BigDecimal price,

        @NotNull(message = "quantity khong duoc null")
        @Min(value = 0, message = "quantity phai >= 0")
        Integer quantity
) {
}
