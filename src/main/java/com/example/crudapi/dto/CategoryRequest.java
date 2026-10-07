package com.example.crudapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload cho POST / PUT category. */
public record CategoryRequest(

        @NotBlank(message = "name khong duoc de trong")
        @Size(max = 100, message = "name toi da 100 ky tu")
        String name,

        @Size(max = 500, message = "description toi da 500 ky tu")
        String description
) {
}
