package com.bank.overdraft.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(OverdraftException.class)
    public ResponseEntity<ApiError> handleDomain(OverdraftException ex, HttpServletRequest request) {
        log.warn("Domain error [{}]: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiError.of(ex.getStatus().value(), ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return new ApiError.FieldViolation(field, v.getMessage());
                })
                .toList();
        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED",
                "Request validation failed", request.getRequestURI(), violations);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "INTERNAL_ERROR", "An unexpected error occurred", request.getRequestURI()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.FieldViolation(f.getField(), f.getDefaultMessage()))
                .toList();
        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED",
                "Request validation failed", pathOf(request), violations);
        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "MALFORMED_REQUEST", "Request body is missing or malformed", pathOf(request)));
    }

    private String pathOf(WebRequest request) {
        return request.getDescription(false).replaceFirst("^uri=", "");
    }
}
