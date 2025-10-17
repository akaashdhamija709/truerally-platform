package com.truerally.common.exception;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Counter clientErrorCounter;
    private final Counter serverErrorCounter;

    public GlobalExceptionHandler(MeterRegistry registry) {
        this.clientErrorCounter = registry.counter("truerally.errors.client");
        this.serverErrorCounter = registry.counter("truerally.errors.server");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                details.put(err.getField(), err.getDefaultMessage())
        );
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex, request, details);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntime(RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, Exception ex, HttpServletRequest request) {
        return buildErrorResponse(status, ex, request, null);
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, Exception ex,
                                                                HttpServletRequest request, Map<String, Object> details) {
        String correlationId = MDC.get("correlationId");
        Long startTime = (Long) request.getAttribute("startTime");
        long latencyMs = (startTime != null) ? (System.currentTimeMillis() - startTime) : 0L;
        String path = request.getRequestURI();

        ApiErrorResponse response = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                path,
                correlationId,
                details
        );

        if (status.is4xxClientError()) {
            log.warn("event=client_error status={} path={} cid={} latency={} message={}",
                    status.value(), path, correlationId, latencyMs, ex.getMessage());
            clientErrorCounter.increment();
        } else if (status.is5xxServerError()) {
            log.error("event=server_error status={} path={} cid={} latency={} message={}",
                    status.value(), path, correlationId, latencyMs, ex.getMessage(), ex);
            serverErrorCounter.increment();
        }

        return ResponseEntity.status(status).body(response);
    }
}
