package com.example.crudapi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Chuyen exception thanh response chuan RFC 7807 (ProblemDetail).
 * Nho vay controller/service khong phai tu build ResponseEntity loi.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        return problem(HttpStatus.CONFLICT, "Duplicate resource", ex.getMessage());
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ProblemDetail handleResourceInUse(ResourceInUseException ex) {
        return problem(HttpStatus.CONFLICT, "Resource in use", ex.getMessage());
    }

    /** PATCH nhan JsonNode nen khong di qua @Valid duoc; service tu kiem tra va nem loi nay. */
    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
    }

    /** Loi @Valid tren @RequestBody -> 400 kem chi tiet tung field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed", "Du lieu gui len khong hop le");
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    /**
     * Body khong parse duoc: sai cu phap JSON, sai kieu du lieu ("price": "abc"),
     * hoac gia tri khong thuoc enum ("role": "SUPERADMIN"). Jackson nem exception nay
     * TRUOC khi @Valid chay, nen no khong the thanh MethodArgumentNotValidException.
     * Khong co handler nay thi no roi xuong handleUnexpected va tra 500 cho mot loi dau vao.
     *
     * Khong dua ex.getMessage() ra ngoai vi no chua ten class/package noi bo.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Body khong doc duoc", ex);
        return problem(HttpStatus.BAD_REQUEST, "Malformed request body",
                "Body khong doc duoc: sai cu phap JSON hoac sai kieu du lieu");
    }

    /**
     * Query param sai kieu du lieu (vi du ?categoryId=abc) -> loi dau vao, khong phai loi server.
     * Khong co handler nay thi no roi xuong handleUnexpected va tra ve 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid parameter type",
                "Tham so '" + ex.getName() + "' khong dung dinh dang");
    }

    /**
     * Client gui ten field sort khong ton tai (vi du ?sort=abc) -> loi dau vao, khong phai loi server.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleInvalidSortProperty(PropertyReferenceException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid sort property",
                "Khong ton tai thuoc tinh de sap xep: " + ex.getPropertyName());
    }

    /**
     * Luoi an toan cuoi cung cho rang buoc o tang DB (unique, not null, FK...)
     * khi kiem tra o tang service bi race condition vuot qua.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Vi pham rang buoc du lieu", ex);
        return problem(HttpStatus.CONFLICT, "Data integrity violation",
                "Du lieu vi pham rang buoc cua database");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentication failed", "Sai username hoac password");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Concurrent modification",
                "Ban ghi vua bi nguoi khac thay doi, vui long tai lai va thu lai");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Loi khong mong doi", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "Da co loi xay ra, vui long thu lai sau");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
