package com.truerally.auth.controller;

import com.truerally.auth.dto.*;
import com.truerally.auth.dto.PublicLoginResponse;
import com.truerally.auth.exception.InvalidTokenException;
import com.truerally.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<VerifyResponse> verify(@RequestParam("token") String token) {
        VerifyResponse response = authService.verify(new VerifyRequest(token));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<PublicLoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        // Create refresh token cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(true)             // true in prod (requires HTTPS)
                .path("/auth")    // cookie is only sent to refresh/logout endpoints
                .maxAge(24 * 60 * 60) // 1 day
                .sameSite("Strict")       // or "Lax" depending on your app
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(response.toPublicResponse()); // strip refresh token from body
    }

    @PostMapping("/refresh")
    public ResponseEntity<PublicRefreshResponse> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new InvalidTokenException("Missing refresh token");
        }
        RefreshResponse response = authService.refresh(new RefreshRequest(refreshToken));
        // Set new refresh token cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/auth")
                .maxAge(24 * 60 * 60) // 1 day
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(response.toPublicResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new InvalidTokenException("Missing refresh token");
        }

        LogoutResponse response = authService.logout(new LogoutRequest(refreshToken));

        // Expire the cookie on client side
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/auth")
                .maxAge(0) // delete immediately
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", deleteCookie.toString())
                .body(response);
    }
    @PostMapping("/reset-password/request")
    public ResponseEntity<?> requestReset(@Valid @RequestBody ResetPasswordRequest request) {
        // TODO: implement logic
        return ResponseEntity.ok(Map.of("message", "Password reset email sent if account exists"));
    }

    @GetMapping("/reset-password/validate")
    public ResponseEntity<ValidateResetTokenResponse> validateResetToken(@RequestParam String token) {
        // TODO: implement logic
        return ResponseEntity.ok(new ValidateResetTokenResponse("Token is valid", true));
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<ConfirmResetResponse> confirmReset(@Valid @RequestBody ConfirmResetRequest request) {
        // TODO: implement logic
        return ResponseEntity.ok(new ConfirmResetResponse("Password reset successful"));
    }

}
