package com.truerally.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truerally.common.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    private final ObjectMapper mapper;

    public JwtAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String correlationId = MDC.get("correlationId");
        Long startTime = (Long) request.getAttribute("startTime");
        long latencyMs = (startTime != null) ? (System.currentTimeMillis() - startTime) : 0L;

        String message = (authException != null && authException.getMessage() != null)
                ? authException.getMessage()
                : "Unauthorized access";
        String path = request.getRequestURI();

        // Build standardized error body using ApiErrorResponse (shared model)
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                path,
                correlationId,
                Map.of(
                        "latencyMs", latencyMs
                )
        );

        // Write structured JSON response
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(), errorResponse);

        // Structured log entry (no stack trace)
        log.warn("event=unauthorized_access method={} path={} cid={} latency={}ms message={}",
                request.getMethod(), path, correlationId, latencyMs, message);
    }
}
