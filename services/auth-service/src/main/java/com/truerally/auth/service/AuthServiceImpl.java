package com.truerally.auth.service;

import com.truerally.auth.dto.*;
import com.truerally.auth.exception.AccountNotVerifiedException;
import com.truerally.auth.exception.InvalidCredentialsException;
import com.truerally.auth.exception.InvalidTokenException;
import com.truerally.auth.exception.UserAlreadyExistsException;
import com.truerally.auth.model.User;
import com.truerally.auth.model.UserToken;
import com.truerally.auth.repository.UserRepository;
import com.truerally.auth.repository.UserTokenRepository;
import com.truerally.auth.util.EmailTemplateBuilder;
import com.truerally.auth.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final MailService mailService;
    private final EmailTemplateBuilder emailTemplateBuilder;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public RegisterResponse register(RegisterRequest request) {
        log.info("event=register_attempt email={} timestamp={}", request.getEmail(), Instant.now());

        // 1. Check if email exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("event=register_failed email={} reason=email_already_registered timestamp={}", request.getEmail(), Instant.now());
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        // 2. Create user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setDob(request.getDob());
        user.setCity(request.getCity());
        user.setCountry(request.getCountry());
        user.setPincode(request.getPincode());
        user.setGender(request.getGender());
        user.setVerified(false);

        user = userRepository.save(user);

        // 3. Generate verification token
        UserToken token = new UserToken();
        String tokenValue = UUID.randomUUID().toString();
        token.setToken(tokenValue);
        token.setType("VERIFICATION");
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        token.setUser(user);
        userTokenRepository.save(token);

        // 4️⃣ Send verification email
        try {
            String htmlBody = emailTemplateBuilder.buildVerificationEmail(
                    user.getFullName() != null ? user.getFullName() : "User",
                    tokenValue
            );
            mailService.sendMail(user.getEmail(), "Verify your TrueRally account", htmlBody);
            log.info("event=verification_email_sent email={} timestamp={}", user.getEmail(), Instant.now());
        } catch (Exception e) {
            log.error("event=verification_email_failed email={} error={} timestamp={}",
                    user.getEmail(), e.getMessage(), Instant.now());
            // Optional: you can decide to delete user + token if email fails
        }

        log.info("event=register_success userId={} email={} verificationTokenIssued=true timestamp={}",
                user.getId(), user.getEmail(), Instant.now());
        return new RegisterResponse("User registered successfully. Please verify email.", user.getEmail());
    }

    @Override
    @Transactional
    public VerifyResponse verify(VerifyRequest request) {

        log.info("event=verify_attempt tokenId={} timestamp={}", request.getToken(), Instant.now());

        UserToken userToken = userTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (!"VERIFICATION".equals(userToken.getType())) {
            log.warn("event=verify_failed reason=invalid_token_type expected=VERIFICATION found={} timestamp={}",
                    userToken.getType(), Instant.now());
            throw new InvalidTokenException("Invalid token type");
        }

        if (userToken.isUsed()) {
            log.warn("event=verify_failed reason=token used timestamp={}", Instant.now());
            throw new InvalidTokenException("Token already used");
        }

        if (userToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("event=verify_failed reason=token expired timestamp={}", Instant.now());
            throw new InvalidTokenException("Token expired");
        }

        User user = userToken.getUser();
        user.setVerified(true);
        userRepository.save(user);

        userToken.setUsed(true);
        userTokenRepository.save(userToken);

        log.info("event=verify_success userId={} email={} timestamp={}", user.getId(), user.getEmail(), Instant.now());
        return new VerifyResponse("User verified successfully", user.getEmail());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        //validate email and password
        //check verified=true
        //Issue JWT access token(short-lived) + refresh token(long-lived)

        log.info("event=login_attempt email={} timestamp={}", request.getEmail(), Instant.now());

        Optional<User> optUser = userRepository.findByEmail(request.getEmail());

        if (optUser.isEmpty()) {
            log.warn("event=login_failed email={} reason=email_not_found timestamp={}", request.getEmail(), Instant.now());
            throw new InvalidCredentialsException();
        }

        User user = optUser.get();

        if (!user.isVerified()) {
            log.warn("event=login_failed email={} reason=email_not_verified timestamp={}", request.getEmail(), Instant.now());
            throw new AccountNotVerifiedException();
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("event=login_failed email={} reason=invalid_password timestamp={}", request.getEmail(), Instant.now());
            throw new InvalidCredentialsException();
        }

        // Create Access Token
        Map<String, Object> claims = Map.of(
                "email", user.getEmail(),
                "fullName", user.getFullName()
        );
        String accessToken = jwtUtil.generateAccessToken(user.getId().toString(), claims);

        // Create Refresh Token (UUID stored in DB)
        String refreshToken = UUID.randomUUID().toString();
        UserToken tokenEntity = new UserToken();
        tokenEntity.setUser(user);
        tokenEntity.setToken(refreshToken);
        tokenEntity.setType("REFRESH");
        tokenEntity.setCreatedAt(LocalDateTime.now());
        tokenEntity.setExpiresAt(LocalDateTime.now().plusDays(1)); // 1 day refresh token
        tokenEntity.setUsed(false);

        userTokenRepository.save(tokenEntity);
        log.info("event=login_success userId={} email={} accessTokenIssued=true refreshTokenIssued=true timestamp={}",
                user.getId(), user.getEmail(), Instant.now());
        return new LoginResponse("Login successful", user.getEmail(), accessToken, refreshToken, 300); // 300s = 5min
    }

    @Override
    public RefreshResponse refresh(RefreshRequest request) {

        log.info("event=refresh_attempt tokenId={} timestamp={}", request.getRefreshToken(), Instant.now());
        UserToken token = userTokenRepository.findByTokenAndType(request.getRefreshToken(), "REFRESH")
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (token.isUsed() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("event=refresh_failed tokenId={} reason=token_used_or_expired timestamp={}", request.getRefreshToken(), Instant.now());
            throw new InvalidTokenException("Refresh token expired or already used");
        }

        User user = token.getUser();

        // mark old refresh token used
        token.setUsed(true);
        userTokenRepository.save(token);

        // create new tokens
        String newAccessToken = jwtUtil.generateAccessToken(user.getId().toString(),
                Map.of("email", user.getEmail(),
                        "fullName", user.getFullName()));

        String newRefreshToken = UUID.randomUUID().toString();
        UserToken newRefresh = new UserToken();
        newRefresh.setToken(newRefreshToken);
        newRefresh.setType("REFRESH");
        newRefresh.setUser(user);
        newRefresh.setCreatedAt(LocalDateTime.now());
        newRefresh.setExpiresAt(LocalDateTime.now().plusDays(1)); // 1 day refresh token
        newRefresh.setUsed(false);
        userTokenRepository.save(newRefresh);

        log.info("event=refresh_success userId={} email={} timestamp={}", user.getId(), user.getEmail(), Instant.now());
        return new RefreshResponse("Token refreshed successfully", user.getEmail(), newAccessToken, newRefreshToken, 300);
    }

    @Override
    public LogoutResponse logout(LogoutRequest request) {
        log.info("event=logout_attempt tokenId={} timestamp={}", request.getRefreshToken(), Instant.now());
        UserToken token = userTokenRepository.findByTokenAndType(request.getRefreshToken(), "REFRESH")
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (token.isUsed()) {
            log.warn("event=logout_failed tokenId={} reason=token_used_or_expired timestamp={}", request.getRefreshToken(), Instant.now());
            throw new InvalidTokenException("Refresh token expired or already used");
        }

        token.setUsed(true);
        userTokenRepository.save(token);
        log.info("event=logout_success userId={} email={} timestamp={}", token.getUser().getId(), token.getUser().getEmail(), Instant.now());
        return new LogoutResponse("User logged out successfully!", token.getUser().getEmail());
    }

    @Override
    public void requestReset(ResetPasswordRequest request) {
        // TODO: generate token + save + send email
    }

    @Override
    public ValidateResetTokenResponse validateToken(String token) {
        // TODO: check DB for token validity
        return new ValidateResetTokenResponse("Token is valid (placeholder)", true);
    }

    @Override
    public ConfirmResetResponse confirmReset(ConfirmResetRequest request) {
        // TODO: validate token + update password + mark token used
        return new ConfirmResetResponse("Password reset successful (placeholder)");
    }
}
