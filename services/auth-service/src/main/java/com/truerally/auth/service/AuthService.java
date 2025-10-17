package com.truerally.auth.service;

import com.truerally.auth.dto.*;
import com.truerally.auth.model.User;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    VerifyResponse verify(VerifyRequest token);
    LoginResponse login(LoginRequest request);
    RefreshResponse refresh(RefreshRequest refreshToken);
    LogoutResponse logout(LogoutRequest refreshToken);
    void requestReset(ResetPasswordRequest request);
    ValidateResetTokenResponse validateToken(String token);
    ConfirmResetResponse confirmReset(ConfirmResetRequest request);
}
