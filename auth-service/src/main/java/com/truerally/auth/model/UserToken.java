package com.truerally.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_tokens", schema = "auth")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@DynamicUpdate
public class UserToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String token;

    private String type; // VERIFICATION / RESET_PASSWORD / REFRESH

    private boolean isUsed = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
