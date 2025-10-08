package com.truerally.auth.config;

import com.truerally.auth.exception.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Counter clientErrorCounter;
    private final Counter serverErrorCounter;

    public GlobalExceptionHandler(MeterRegistry registry) {
        this.clientErrorCounter = registry.counter("truerally.errors.client");
        this.serverErrorCounter = registry.counter("truerally.errors.server");
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status, HttpServletRequest request) {
        String correlationId = org.slf4j.MDC.get("correlationId");
        long startTime = (long) request.getAttribute("startTime");
        long latencyMs = System.currentTimeMillis() - startTime;
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                correlationId,
                latencyMs
        );

        if (status.is4xxClientError()) {
            log.warn("Client error [{} {}] cid={} latency={} msg={}",
                    request.getMethod(), request.getRequestURI(), correlationId, latencyMs, ex.getMessage());
            clientErrorCounter.increment();
        } else if (status.is5xxServerError()) {
            log.error("Server error [{} {}] cid={} latency={} msg={}",
                    request.getMethod(), request.getRequestURI(), correlationId, latencyMs, ex.getMessage(), ex);
            serverErrorCounter.increment();
        }

        return new ResponseEntity<>(errorResponse, status);
    }

    // 400: validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        StringBuilder messages = new StringBuilder();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> messages.append(error.getField())
                        .append(": ")
                        .append(error.getDefaultMessage())
                        .append("; "));

        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    // 409: email exists
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(
            UserAlreadyExistsException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    // 400: invalid role
    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRole(
            InvalidRoleException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    // 401: unauthorized
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request);
    }

    // 403: disabled/forbidden
    @ExceptionHandler(UserDisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(
            UserDisabledException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, request);
    }

    // generic 400 fallback
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    // generic 500 fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<?> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(AccountNotVerifiedException.class)
    public ResponseEntity<?> handleAccountNotVerified(
            AccountNotVerifiedException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, request);
    }

}
