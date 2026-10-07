package com.example.crudapi.exception;

/** Nem khi du lieu dau vao sai dinh dang/gia tri nhung khong di qua duoc Bean Validation (vi du PATCH). */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
