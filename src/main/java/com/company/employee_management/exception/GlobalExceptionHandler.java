package com.company.employee_management.exception;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(
            InvalidRequestException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(
                LocalDateTime.now(), 400, "Bad Request", exception.getMessage(), request.getRequestURI(), Map.of()));
    }

    @ExceptionHandler(DuplicateDepartmentException.class)
    public ResponseEntity<ApiError> handleDuplicateDepartment(
            DuplicateDepartmentException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                LocalDateTime.now(), 409, "Conflict", exception.getMessage(), request.getRequestURI(), Map.of()));
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiError> handleInvalidRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(
                LocalDateTime.now(), 400, "Bad Request",
                "Invalid request: check path values, query parameters and JSON body",
                request.getRequestURI(), Map.of()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        exception.getParameterValidationResults().forEach(result ->
                result.getResolvableErrors().forEach(error -> {
                    String name = error instanceof FieldError fieldError
                            ? fieldError.getField() : result.getMethodParameter().getParameterName();
                    errors.put(name == null ? "request" : name, error.getDefaultMessage());
                }));
        return ResponseEntity.badRequest().body(new ApiError(
                LocalDateTime.now(), 400, "Bad Request", "Request validation failed",
                request.getRequestURI(), errors));
    }

    @ExceptionHandler({DataIntegrityViolationException.class,
            org.springframework.dao.OptimisticLockingFailureException.class})
    public ResponseEntity<ApiError> handleDataConflict(
            RuntimeException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                LocalDateTime.now(), 409, "Conflict",
                "The operation conflicts with existing data or a concurrent update; reload and retry",
                request.getRequestURI(), Map.of()));
    }

    @ExceptionHandler({EmployeeNotFoundException.class, DepartmentNotFoundException.class})
    public ResponseEntity<ApiError> handleEmployeeNotFound(
            RuntimeException exception,
            HttpServletRequest request
    ) {

        ApiError error = new ApiError(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(DuplicateEmployeeException.class)
    public ResponseEntity<ApiError> handleDuplicateEmployee(
            DuplicateEmployeeException exception,
            HttpServletRequest request
    ) {

        ApiError error = new ApiError(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        Map<String, String> validationErrors =
                new HashMap<>();

        exception
                .getBindingResult()
                .getFieldErrors()
                .forEach(fieldError ->
                        validationErrors.put(
                                fieldError.getField(),
                                fieldError.getDefaultMessage()
                        )
                );

        ApiError error = new ApiError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Request validation failed",
                request.getRequestURI(),
                validationErrors
        );

        return ResponseEntity
                .badRequest()
                .body(error);
    }
}
