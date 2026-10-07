package com.example.crudapi.exception;

/** Nem khi ban ghi dang duoc ban ghi khac tham chieu nen khong the xoa -> HTTP 409. */
public class ResourceInUseException extends RuntimeException {

    public ResourceInUseException(String message) {
        super(message);
    }
}
