package com.example.crudapi.exception;

/** Nem khi vi pham rang buoc duy nhat (vi du sku da ton tai) -> HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
