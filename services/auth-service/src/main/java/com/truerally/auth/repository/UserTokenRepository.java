package com.truerally.auth.repository;

import com.truerally.auth.model.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserTokenRepository extends JpaRepository<UserToken, UUID> {
    Optional<UserToken> findByToken(String token);

    Optional<UserToken> findByTokenAndType(String token, String type);

    Optional<UserToken> findByTokenAndTypeAndIsUsedFalse(String token, String type);
}
