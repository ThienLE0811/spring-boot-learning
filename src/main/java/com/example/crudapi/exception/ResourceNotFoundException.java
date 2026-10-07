package com.example.crudapi.exception;

/** Nem khi khong tim thay ban ghi -> HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
